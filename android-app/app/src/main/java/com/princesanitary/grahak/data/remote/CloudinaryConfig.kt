package com.princesanitary.grahak.data.remote

/**
 * Configuration for Cloudinary Image Hosting.
 *
 * Cloudinary offers 25 GB of free image storage and CDN bandwidth per month
 * with no credit card required.
 *
 * Setup:
 * 1. Log in to your Cloudinary Console (https://cloudinary.com/console).
 * 2. Copy your "Cloud Name" from the Dashboard.
 * 3. Go to Settings -> Upload -> Upload presets -> Add upload preset:
 *    - Signing Mode: Unsigned
 *    - Save and copy the Preset Name (e.g. "grahak_products").
 */
object CloudinaryConfig {
    /**
     * Your Cloudinary Cloud Name.
     */
    var CLOUD_NAME: String = "dktirbsyc"

    /**
     * The Unsigned Upload Preset name configured in your Cloudinary settings.
     */
    var UPLOAD_PRESET: String = "grahak_fixit"

    /**
     * Optional folder in Cloudinary to organize product images.
     * Leave blank if your unsigned preset manages folders automatically.
     */
    const val PRODUCT_FOLDER: String = ""
}
