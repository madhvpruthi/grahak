# Adding products to FixIt (store owner)

Customers see a product when a document exists in the `catalog` collection with
`isPublished = true`. There are two ways to create those.

---

## Step 0 (one time): make yourself an admin

The security rules only allow writes to `catalog` from an admin account. An
"admin" is simply an account that has a document in the `admins` collection.

This cannot be automated — the rules deliberately block all writes to `admins`
so nobody can promote themselves. Do it by hand once:

1. Open [Firestore data](https://console.firebase.google.com/project/grahakdukaan/firestore/data)
2. **Start collection** → Collection ID: `admins`
3. Document ID: your account's UID

   | Account | UID |
   |---|---|
   | `owner@example.com` | `<YOUR_FIREBASE_AUTH_UID>` |

4. Add field: `role` (string) = `owner`
5. Save

---

## Option A: bulk upload with the script (fastest)

A ready-made script lives at `firebase/seed-catalog.mjs` with ten realistic
sanitary-store products already filled in.

```bash
cd firebase
node seed-catalog.mjs owner@example.com YOUR_PASSWORD
```

Output looks like:

```
Signing in…
  signed in as <YOUR_FIREBASE_AUTH_UID>
  ✓ PVC Pipe 1 inch (3m)
  ✓ CPVC Pipe 3/4 inch (3m)
  ...
Done. 10/10 products published.
```

To add your real stock, edit the `PRODUCTS` array at the top of that file and
re-run. Products are keyed by `sku`, so re-running **updates** rather than
duplicates — which also makes it a convenient way to change prices or stock.

---

## Option B: add one product by hand in the console

Go to [Firestore data](https://console.firebase.google.com/project/grahakdukaan/firestore/data)
→ collection `catalog` → **Add document**.

Document ID: anything stable, e.g. `pvc-pipe-1in`.

Fields must match exactly — the app maps these onto `Product.kt`, and a wrong
type means the product silently fails to load:

| Field | Type | Example | Notes |
|---|---|---|---|
| `name` | string | `PVC Pipe 1 inch (3m)` | shown on the card |
| `brand` | string | `Supreme` | small label above the name |
| `categoryId` | string | `pipes` | **must** be `pipes`, `fittings`, `sanitaryware` or `tools` |
| `pricingType` | string | `FIXED` | `FIXED` or `RFQ_ONLY` |
| `unitPrice` | number | `245` | ignored when `RFQ_ONLY` |
| `minOrderQty` | number | `1` | shown as "min 5" when > 1 |
| `stockLevel` | number | `120` | `0` renders an OUT OF STOCK badge |
| `isPublished` | boolean | `true` | **false hides it from customers** |
| `mediaUrls` | array of string | `[]` | first entry is the card image |
| `specifications` | map (string → string) | `Length: 3 m` | shown on the detail screen |
| `tax` | map | see below | |

`tax` is a nested map with two fields:

| Field | Type | Example |
|---|---|---|
| `hsn` | string | `3917` |
| `gstRate` | number | `18` |

---

## The two pricing modes

This is the core of the catalogue, so it's worth being deliberate:

- **`FIXED`** — the price is visible and the customer can add it straight to the
  cart with the `ADD` button. Use this for standard stock with a stable counter
  rate.
- **`RFQ_ONLY`** — no price is shown; the customer sees **Request quote**
  instead. Use this for anything where the price depends on size, brand,
  negotiation or current market rate.

---

## Product images

`mediaUrls` takes full https URLs. Firebase Storage is the natural home, but
**Storage is not yet enabled on this project** — a deploy attempt failed with
*"Firebase Storage has not been set up"*.

To enable it:

1. Open [Storage](https://console.firebase.google.com/project/grahakdukaan/storage)
   and click **Get started**
2. Then, from the repo:
   ```bash
   cd firebase && firebase deploy --only storage
   ```

Until then, either leave `mediaUrls` empty (the app draws a clean placeholder
with the product initials) or paste any public image URL.

Enabling Storage is also required before plumbers can upload their KYC
documents during registration.

---

## Checking it worked

1. Open the app as a customer
2. Pull up the **Shop** tab

Products are served by a live Firestore listener, so new items appear without
restarting the app. If the list stays empty, the usual causes are:

- `isPublished` is `false`, or was saved as the *string* `"true"` instead of a boolean
- `categoryId` doesn't match one of the four allowed values
- `unitPrice` / `stockLevel` saved as strings instead of numbers

---

## Longer term

Doing this from the Firebase console works but won't scale for daily stock
updates. Product management belongs in the **Dukaan business app**, which is
where you said the admin side should live. The pieces the app already expects
from it are:

- create / edit / publish `catalog` documents
- flip `kyc.isVerified` on `plumbers/{uid}` to approve a plumber
- advance `orders/{id}.status` through `CONFIRMED → PACKED → SHIPPED → DELIVERED`
- approve `withdrawal_requests` and mark them `COMPLETED`

The Firestore rules already reserve every one of those actions for admins.
