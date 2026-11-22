#!/usr/bin/env node
/*
 * Plumber approval tool (store owner).
 *
 * A plumber who signs up lands in PENDING and cannot accept work or appear to
 * customers until the store approves them - the same gate a ride-hailing app
 * puts on a new driver. This is how the store does that.
 *
 * Requires the one-time admin step from STORE_OWNER_GUIDE.md: a document at
 * admins/{your-uid}. Firestore rules reserve plumber approval for admins.
 *
 * USAGE
 *   node plumbers.mjs list    <owner-email> <owner-password>
 *   node plumbers.mjs approve <owner-email> <owner-password> <plumberId>
 *   node plumbers.mjs reject  <owner-email> <owner-password> <plumberId> "reason"
 */

const API_KEY = 'AIzaSyDIXQF6oK859aSHLVJyOKIb9d6IESho4zo';
const PROJECT = 'grahakdukaan';
const FS = `https://firestore.googleapis.com/v1/projects/${PROJECT}/databases/(default)/documents`;

async function signIn(email, password) {
  const res = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${API_KEY}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, returnSecureToken: true }),
    }
  );
  const body = await res.json();
  if (!res.ok) throw new Error(body.error?.message ?? `HTTP ${res.status}`);
  return body.idToken;
}

/** Firestore typed-value -> plain JS. */
function fromValue(v) {
  if (!v) return null;
  if ('stringValue' in v) return v.stringValue;
  if ('booleanValue' in v) return v.booleanValue;
  if ('integerValue' in v) return Number(v.integerValue);
  if ('doubleValue' in v) return v.doubleValue;
  if ('nullValue' in v) return null;
  if ('arrayValue' in v) return (v.arrayValue.values ?? []).map(fromValue);
  if ('mapValue' in v) return fromFields(v.mapValue.fields ?? {});
  if ('timestampValue' in v) return v.timestampValue;
  return null;
}

function fromFields(fields) {
  return Object.fromEntries(
    Object.entries(fields).map(([k, v]) => [k, fromValue(v)])
  );
}

async function listPlumbers(token) {
  const res = await fetch(`${FS}/plumbers?pageSize=100`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const body = await res.json();
  if (!res.ok) throw new Error(body.error?.message ?? `HTTP ${res.status}`);

  const docs = body.documents ?? [];
  if (docs.length === 0) {
    console.log('\nNo plumbers have signed up yet.\n');
    return;
  }

  console.log(`\n${docs.length} plumber(s):\n`);
  for (const doc of docs) {
    const id = doc.name.split('/').pop();
    const d = fromFields(doc.fields ?? {});
    const kyc = d.kyc ?? {};
    const rates = d.rates ?? {};
    const status = kyc.verified
      ? 'APPROVED'
      : (kyc.reviewStatus ?? 'PENDING');

    console.log(`  ${status.padEnd(9)} ${d.name ?? '(no name)'}  [${id}]`);
    console.log(`            phone: ${d.phone ?? '-'}   area: ${d.baseAddress ?? d.servicePincode ?? '-'}`);
    console.log(`            visit: ₹${rates.visitCharge ?? 0}   services: ${(d.specialities ?? []).join(', ') || '-'}`);
    const custom = rates.customRates ?? {};
    const rateList = Object.entries(custom).map(([k, v]) => `${k}=₹${v}`);
    if (rateList.length) console.log(`            rates: ${rateList.join('  ')}`);
    console.log('');
  }
  console.log('Approve with:  node plumbers.mjs approve <email> <password> <plumberId>\n');
}

async function setReview(token, plumberId, approved, reason = '') {
  // Only the three KYC fields are touched, so rates and profile stay intact.
  const mask = [
    'kyc.verified',
    'kyc.reviewStatus',
    'kyc.rejectionReason',
  ].map((f) => `updateMask.fieldPaths=${encodeURIComponent(f)}`).join('&');

  const res = await fetch(`${FS}/plumbers/${plumberId}?${mask}`, {
    method: 'PATCH',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      fields: {
        kyc: {
          mapValue: {
            fields: {
              verified: { booleanValue: approved },
              reviewStatus: { stringValue: approved ? 'APPROVED' : 'REJECTED' },
              rejectionReason: { stringValue: approved ? '' : reason },
            },
          },
        },
      },
    }),
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error?.message ?? `HTTP ${res.status}`);
  }
}

async function main() {
  const [command, email, password, plumberId, reason] = process.argv.slice(2);

  if (!command || !email || !password) {
    console.error(
      'Usage:\n' +
        '  node plumbers.mjs list    <owner-email> <owner-password>\n' +
        '  node plumbers.mjs approve <owner-email> <owner-password> <plumberId>\n' +
        '  node plumbers.mjs reject  <owner-email> <owner-password> <plumberId> "reason"'
    );
    process.exit(1);
  }

  const token = await signIn(email, password);

  switch (command) {
    case 'list':
      await listPlumbers(token);
      break;

    case 'approve':
      if (!plumberId) throw new Error('Pass the plumberId (see `list`)');
      await setReview(token, plumberId, true);
      console.log(`\n✓ Approved ${plumberId}.`);
      console.log('  Their app switches out of "Under review" immediately, and');
      console.log('  they now appear to customers once they go on duty.\n');
      break;

    case 'reject':
      if (!plumberId) throw new Error('Pass the plumberId (see `list`)');
      await setReview(token, plumberId, false, reason ?? 'Details could not be verified');
      console.log(`\n✓ Rejected ${plumberId}.\n`);
      break;

    default:
      throw new Error(`Unknown command "${command}"`);
  }
}

main().catch((e) => {
  console.error(`\nError: ${e.message}`);
  if (/permission|PERMISSION/i.test(e.message)) {
    console.error(
      '\nThis needs an admin document. In the Firebase console create:\n' +
        '  Collection: admins   Document ID: <your uid>   Field: role = "owner"\n' +
        'See STORE_OWNER_GUIDE.md.\n'
    );
  }
  process.exit(1);
});
