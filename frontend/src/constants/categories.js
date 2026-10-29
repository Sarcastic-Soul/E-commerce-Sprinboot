/**
 * Mirrors Product.ProductCategory on the backend. The filter dropdown and both
 * admin forms all render from this list, so a new enum constant only has to be
 * added in one place — previously the Navbar drifted to 4 of the 10 categories.
 */
export const PRODUCT_CATEGORIES = [
    { value: "ELECTRONICS", label: "Electronics" },
    { value: "FASHION", label: "Fashion" },
    { value: "HOME_KITCHEN", label: "Home & Kitchen" },
    { value: "BEAUTY_PERSONAL_CARE", label: "Beauty & Personal Care" },
    { value: "BOOKS_STATIONERY", label: "Books & Stationery" },
    { value: "HEALTH_WELLNESS", label: "Health & Wellness" },
    { value: "TOYS_GAMES", label: "Toys & Games" },
    { value: "SPORTS_OUTDOORS", label: "Sports & Outdoors" },
    { value: "AUTOMOTIVE", label: "Automotive" },
    { value: "GROCERIES_GOURMET_FOOD", label: "Groceries & Gourmet Food" },
];

const LABELS = Object.fromEntries(PRODUCT_CATEGORIES.map((c) => [c.value, c.label]));

/** "TOYS_GAMES" -> "Toys & Games"; unknown values are shown as they are. */
export const categoryLabel = (value) => LABELS[value] ?? value;
