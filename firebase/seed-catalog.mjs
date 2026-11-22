#!/usr/bin/env node
/*
 * Adds products to the FixIt catalog.
 *
 * WHY THIS EXISTS
 * ---------------
 * The customer app reads `catalog` documents where isPublished == true. The
 * Firestore rules only allow writes to `catalog` from an admin, i.e. an account
 * that has a document at admins/{uid}. So before this works you must do the
 * one-time step in STORE_OWNER_GUIDE.md ("Make yourself an admin").
 *
 * USAGE
 * -----
 *   node seed-catalog.mjs <owner-email> <owner-password>
 *
 * It signs in as the owner, then writes each product below. Re-running is safe:
 * documents are keyed by `sku`, so an existing product is updated rather than
 * duplicated.
 *
 * Edit the PRODUCTS array to match real stock.
 */

const API_KEY = 'AIzaSyDIXQF6oK859aSHLVJyOKIb9d6IESho4zo';
const PROJECT = 'grahakdukaan';
const FS = `https://firestore.googleapis.com/v1/projects/${PROJECT}/databases/(default)/documents`;

/* ----------------------------------------------------------------- products */
/*
 * pricingType:
 *   FIXED     -> price is shown, customer can add straight to cart
 *   RFQ_ONLY  -> no price shown, customer taps "Request quote"
 *
 * categoryId must be one of: pipes | fittings | sanitaryware | tools
 */
const PRODUCTS = [
  {
    sku: 'pvc-pipe-1in',
    name: 'PVC Pipe 1 inch (3m)',
    brand: 'Supreme',
    categoryId: 'pipes',
    pricingType: 'FIXED',
    unitPrice: 245,
    minOrderQty: 1,
    stockLevel: 120,
    gstRate: 18,
    hsn: '3917',
    specifications: { Length: '3 m', Diameter: '1 inch', Grade: 'ISI' },
  },
  {
    sku: 'cpvc-pipe-075in',
    name: 'CPVC Pipe 3/4 inch (3m)',
    brand: 'Astral',
    categoryId: 'pipes',
    pricingType: 'FIXED',
    unitPrice: 310,
    minOrderQty: 1,
    stockLevel: 80,
    gstRate: 18,
    hsn: '3917',
    specifications: { Length: '3 m', Diameter: '3/4 inch' },
  },
  {
    sku: 'elbow-90-1in',
    name: 'PVC Elbow 90° 1 inch',
    brand: 'Supreme',
    categoryId: 'fittings',
    pricingType: 'FIXED',
    unitPrice: 32,
    minOrderQty: 5,
    stockLevel: 400,
    gstRate: 18,
    hsn: '3917',
    specifications: { Angle: '90°', Size: '1 inch' },
  },
  {
    sku: 'tee-1in',
    name: 'PVC Tee 1 inch',
    brand: 'Finolex',
    categoryId: 'fittings',
    pricingType: 'FIXED',
    unitPrice: 41,
    minOrderQty: 5,
    stockLevel: 260,
    gstRate: 18,
    hsn: '3917',
    specifications: { Size: '1 inch' },
  },
  {
    sku: 'ball-valve-brass-1in',
    name: 'Brass Ball Valve 1 inch',
    brand: 'Zoloto',
    categoryId: 'fittings',
    pricingType: 'FIXED',
    unitPrice: 520,
    minOrderQty: 1,
    stockLevel: 45,
    gstRate: 18,
    hsn: '8481',
    specifications: { Material: 'Brass', Size: '1 inch' },
  },
  {
    sku: 'wash-basin-white',
    name: 'Wall Mounted Wash Basin',
    brand: 'Hindware',
    categoryId: 'sanitaryware',
    pricingType: 'FIXED',
    unitPrice: 2650,
    minOrderQty: 1,
    stockLevel: 12,
    gstRate: 18,
    hsn: '6910',
    specifications: { Colour: 'White', Mounting: 'Wall' },
  },
  {
    sku: 'western-toilet-s-trap',
    name: 'Western Toilet (S-Trap)',
    brand: 'Cera',
    categoryId: 'sanitaryware',
    pricingType: 'RFQ_ONLY',
    unitPrice: 0,
    minOrderQty: 1,
    stockLevel: 6,
    gstRate: 18,
    hsn: '6910',
    specifications: { Trap: 'S-Trap', Colour: 'White' },
  },
  {
    sku: 'shower-set-chrome',
    name: 'Overhead Shower Set (Chrome)',
    brand: 'Jaquar',
    categoryId: 'sanitaryware',
    pricingType: 'FIXED',
    unitPrice: 1890,
    minOrderQty: 1,
    stockLevel: 18,
    gstRate: 18,
    hsn: '8481',
    specifications: { Finish: 'Chrome' },
  },
  {
    sku: 'pipe-wrench-12',
    name: 'Pipe Wrench 12 inch',
    brand: 'Taparia',
    categoryId: 'tools',
    pricingType: 'FIXED',
    unitPrice: 640,
    minOrderQty: 1,
    stockLevel: 22,
    gstRate: 18,
    hsn: '8204',
    specifications: { Size: '12 inch' },
  },
  {
    sku: 'teflon-tape-pack10',
    name: 'Teflon Tape (Pack of 10)',
    brand: 'Generic',
    categoryId: 'tools',
    pricingType: 'FIXED',
    unitPrice: 95,
    minOrderQty: 1,
    stockLevel: 150,
    gstRate: 12,
    hsn: '3919',
    specifications: { Pack: '10 rolls' },
  },
];

/* -------------------------------------------------------------- firestore -- */

/** Converts a plain JS value into Firestore's typed-value JSON. */
function toValue(v) {
  if (v === null || v === undefined) return { nullValue: null };
  if (typeof v === 'boolean') return { booleanValue: v };
  if (typeof v === 'number') return { doubleValue: v };
  if (typeof v === 'string') return { stringValue: v };
  if (Array.isArray(v)) {
    return { arrayValue: { values: v.map(toValue) } };
  }
  if (typeof v === 'object') {
    return { mapValue: { fields: toFields(v) } };
  }
  throw new Error(`Unsupported value: ${v}`);
}

function toFields(obj) {
  return Object.fromEntries(Object.entries(obj).map(([k, v]) => [k, toValue(v)]));
}

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
  if (!res.ok) {
    throw new Error(`Sign-in failed: ${body.error?.message ?? res.status}`);
  }
  return { token: body.idToken, uid: body.localId };
}

async function upsertProduct(token, p) {
  // Shape must match data/model/Product.kt exactly.
  const doc = {
    name: p.name,
    brand: p.brand,
    categoryId: p.categoryId,
    mediaUrls: p.mediaUrls ?? [],
    specifications: p.specifications ?? {},
    pricingType: p.pricingType,
    unitPrice: p.unitPrice,
    minOrderQty: p.minOrderQty,
    stockLevel: p.stockLevel,
    isPublished: true,
    tax: { hsn: p.hsn ?? '', gstRate: p.gstRate ?? 0 },
  };

  const mask = Object.keys(doc)
    .map((k) => `updateMask.fieldPaths=${k}`)
    .join('&');

  const res = await fetch(`${FS}/catalog/${p.sku}?${mask}`, {
    method: 'PATCH',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ fields: toFields(doc) }),
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(
      `${p.sku}: HTTP ${res.status} ${body.error?.message ?? ''}`.trim()
    );
  }
}

async function main() {
  const [email, password] = process.argv.slice(2);
  if (!email || !password) {
    console.error('Usage: node seed-catalog.mjs <owner-email> <owner-password>');
    process.exit(1);
  }

  console.log('Signing in…');
  const { token, uid } = await signIn(email, password);
  console.log(`  signed in as ${uid}`);

  let ok = 0;
  for (const p of PRODUCTS) {
    try {
      await upsertProduct(token, p);
      console.log(`  ✓ ${p.name}`);
      ok++;
    } catch (e) {
      console.error(`  ✗ ${e.message}`);
      if (String(e.message).includes('403') || /permission/i.test(e.message)) {
        console.error(
          '\n  Permission denied writing to catalog.\n' +
            '  You need an admin document first. In the Firebase console create:\n' +
            `      Collection: admins   Document ID: ${uid}\n` +
            '      Field: role (string) = "owner"\n' +
            '  Then re-run this script.\n'
        );
        break;
      }
    }
  }

  console.log(`\nDone. ${ok}/${PRODUCTS.length} products published.`);
}

main().catch((e) => {
  console.error(e.message);
  process.exit(1);
});
