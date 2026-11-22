import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();
const fcm = admin.messaging();

/**
 * Send notification when a new service job is created
 * Notifies all verified plumbers about new job request
 */
export const onServiceJobCreated = functions.firestore
  .document("service_jobs/{jobId}")
  .onCreate(async (snap, context) => {
    const job = snap.data();
    
    // Skip if not a new open job
    if (job.status !== "OPEN") return null;

    try {
      // Get all verified plumbers with push tokens
      const plumbersSnap = await db.collection("plumbers")
        .where("kyc.verified", "==", true)
        .where("operationalStatus", "==", "AVAILABLE")
        .get();

      const tokens = [];
      plumbersSnap.forEach(doc => {
        const plumber = doc.data();
        if (plumber.fcmToken) {
          tokens.push(plumber.fcmToken);
        }
      });

      if (tokens.length === 0) {
        console.log("No plumbers with FCM tokens found");
        return null;
      }

      // Send notification
      const message = {
        notification: {
          title: "New Job Request",
          body: job.description?.substring(0, 100) || "A customer needs help"
        },
        data: {
          type: "new_job",
          jobId: context.params.jobId,
          customerId: job.customerId || ""
        },
        tokens: tokens
      };

      const response = await fcm.sendEachForMulticast(message);
      console.log(`Sent ${response.successCount} notifications for job ${context.params.jobId}`);

      // Also notify store owner/admins about the new booking
      const adminUsersSnap = await db.collection("users")
        .where("role", "in", ["admin", "owner"])
        .get();
      const adminTokens = [];
      adminUsersSnap.forEach(doc => {
        const u = doc.data();
        if (u.fcmToken) adminTokens.push(u.fcmToken);
      });
      if (adminTokens.length > 0) {
        await fcm.sendEachForMulticast({
          notification: {
            title: "New Plumber Booking Received!",
            body: job.description?.substring(0, 100) || "New customer service request in Saharanpur"
          },
          data: {
            type: "new_booking",
            jobId: context.params.jobId
          },
          tokens: adminTokens
        });
        console.log(`Notified ${adminTokens.length} admins about booking ${context.params.jobId}`);
      }
      
      return null;
    } catch (error) {
      console.error("Error sending job notification:", error);
      return null;
    }
  });

/**
 * Send notification when plumber accepts/updates a job
 * Notifies the customer about job status changes
 */
export const onServiceJobUpdated = functions.firestore
  .document("service_jobs/{jobId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    
    // Only notify on meaningful status changes
    if (before.status === after.status) return null;
    
    const customerId = after.customerId;
    if (!customerId) return null;

    try {
      // Get customer's FCM token
      const userDoc = await db.collection("users").doc(customerId).get();
      const token = userDoc.data()?.fcmToken;
      
      if (!token) {
        console.log("Customer has no FCM token");
        return null;
      }

      // Prepare notification based on status
      let title = "Job Update";
      let body = "";
      
      switch (after.status) {
        case "ACCEPTED":
          const plumberDoc = await db.collection("plumbers").doc(after.plumberId).get();
          const plumberName = plumberDoc.data()?.name || "A plumber";
          body = `${plumberName} has accepted your request`;
          break;
        case "IN_PROGRESS":
          body = "Your plumber is on the way";
          break;
        case "COMPLETED":
          body = "Job completed! Please rate your experience";
          break;
        case "CANCELLED":
          body = "Job has been cancelled";
          break;
        default:
          return null;
      }

      const message = {
        notification: { title, body },
        data: {
          type: "job_update",
          jobId: context.params.jobId,
          status: after.status
        },
        token: token
      };

      await fcm.send(message);
      console.log(`Notified customer ${customerId} about job ${context.params.jobId}`);
      
      return null;
    } catch (error) {
      console.error("Error sending update notification:", error);
      return null;
    }
  });

/**
 * Send notification when a new estimate is created
 * Notifies customer about plumber's quote
 */
export const onEstimateCreated = functions.firestore
  .document("estimates/{estimateId}")
  .onCreate(async (snap, context) => {
    const estimate = snap.data();
    
    try {
      // Get the job to find customer ID
      const jobDoc = await db.collection("service_jobs").doc(estimate.jobId).get();
      const customerId = jobDoc.data()?.customerId;
      
      if (!customerId) return null;

      // Get customer's FCM token
      const userDoc = await db.collection("users").doc(customerId).get();
      const token = userDoc.data()?.fcmToken;
      
      if (!token) return null;

      // Get plumber name
      const plumberDoc = await db.collection("plumbers").doc(estimate.plumberId).get();
      const plumberName = plumberDoc.data()?.name || "A plumber";

      const message = {
        notification: {
          title: "New Estimate Received",
          body: `${plumberName} sent you a quote for ₹${estimate.totalAmount}`
        },
        data: {
          type: "new_estimate",
          estimateId: context.params.estimateId,
          jobId: estimate.jobId
        },
        token: token
      };

      await fcm.send(message);
      console.log(`Notified customer about estimate ${context.params.estimateId}`);
      
      return null;
    } catch (error) {
      console.error("Error sending estimate notification:", error);
      return null;
    }
  });

/**
 * Send notification when a new store order is created.
 * Notifies the store owner/admin about the new customer/plumber order,
 * and notifies the attributed plumber if applicable!
 */
export const onOrderCreated = functions.firestore
  .document("orders/{orderId}")
  .onCreate(async (snap, context) => {
    const order = snap.data();
    const orderId = context.params.orderId;
    const shortId = orderId.substring(0, 8);
    const totalAmount = order.billing?.grandTotal || 0;
    const itemCount = order.items?.length || 0;

    try {
      // 1. Notify store owners/admins
      const adminUsersSnap = await db.collection("users")
        .where("role", "in", ["admin", "owner"])
        .get();

      const adminTokens = [];
      adminUsersSnap.forEach(doc => {
        const u = doc.data();
        if (u.fcmToken) adminTokens.push(u.fcmToken);
      });

      if (adminTokens.length > 0) {
        await fcm.sendEachForMulticast({
          notification: {
            title: "New Store Order Received!",
            body: `Order #${shortId} • ₹${totalAmount} (${itemCount} items) in Saharanpur`
          },
          data: {
            type: "new_order",
            orderId: orderId,
            totalAmount: String(totalAmount)
          },
          tokens: adminTokens
        });
        console.log(`Notified ${adminTokens.length} admins about order ${orderId}`);
      }

      // 2. If order has an attributed plumber, notify plumber about the order & incentive!
      if (order.attributedPlumberId) {
        const plumberDoc = await db.collection("plumbers").doc(order.attributedPlumberId).get();
        const plumberToken = plumberDoc.data()?.fcmToken;
        if (plumberToken) {
          const incentive = order.plumberCommission || ((order.billing?.subtotal || 0) * 0.03);
          await fcm.send({
            notification: {
              title: "Order Placed with 3% Incentive!",
              body: `Order #${shortId} confirmed. ₹${incentive.toFixed(2)} incentive added to your wallet!`
            },
            data: {
              type: "plumber_order_incentive",
              orderId: orderId,
              incentive: String(incentive)
            },
            token: plumberToken
          });
          console.log(`Notified plumber ${order.attributedPlumberId} about order ${orderId}`);
        }
      }

      return null;
    } catch (error) {
      console.error("Error sending order notification:", error);
      return null;
    }
  });

/**
 * Send notification when order status changes
 * Notifies customer about their material order updates
 */
export const onOrderUpdated = functions.firestore
  .document("orders/{orderId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    
    // Only notify on status changes
    if (before.status === after.status) return null;
    
    const customerId = after.customerId;
    if (!customerId) return null;

    try {
      const userDoc = await db.collection("users").doc(customerId).get();
      const token = userDoc.data()?.fcmToken;
      
      if (!token) return null;

      let body = "";
      switch (after.status) {
        case "CONFIRMED":
          body = "Your order has been confirmed";
          break;
        case "SHIPPED":
          body = "Your order is on the way";
          break;
        case "DELIVERED":
          body = "Your order has been delivered";
          break;
        case "CANCELLED":
          body = "Your order has been cancelled";
          break;
        default:
          return null;
      }

      const message = {
        notification: {
          title: "Order Update",
          body: body
        },
        data: {
          type: "order_update",
          orderId: context.params.orderId,
          status: after.status
        },
        token: token
      };

      await fcm.send(message);
      console.log(`Notified customer about order ${context.params.orderId}`);
      
      return null;
    } catch (error) {
      console.error("Error sending order notification:", error);
      return null;
    }
  });

/**
 * Send notification when payout request is processed
 * Notifies plumber about withdrawal status
 */
export const onWithdrawalUpdated = functions.firestore
  .document("withdrawal_requests/{requestId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    
    if (before.status === after.status) return null;
    
    const plumberId = after.plumberId;
    if (!plumberId) return null;

    try {
      const plumberDoc = await db.collection("plumbers").doc(plumberId).get();
      const token = plumberDoc.data()?.fcmToken;
      
      if (!token) return null;

      let body = "";
      switch (after.status) {
        case "APPROVED":
          body = `Your withdrawal of ₹${after.amount} has been approved`;
          break;
        case "COMPLETED":
          body = `₹${after.amount} has been transferred to your account`;
          break;
        case "REJECTED":
          body = `Your withdrawal request was rejected: ${after.rejectionReason || "Please contact support"}`;
          break;
        default:
          return null;
      }

      const message = {
        notification: {
          title: "Withdrawal Update",
          body: body
        },
        data: {
          type: "withdrawal_update",
          requestId: context.params.requestId,
          status: after.status
        },
        token: token
      };

      await fcm.send(message);
      console.log(`Notified plumber ${plumberId} about withdrawal ${context.params.requestId}`);
      
      return null;
    } catch (error) {
      console.error("Error sending withdrawal notification:", error);
      return null;
    }
  });
