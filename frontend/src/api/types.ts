/**
 * App-facing names for the backend's DTOs.
 *
 * Most types come straight from `schema.d.ts`, which `pnpm gen:api` generates from
 * the backend's OpenAPI spec (springdoc at /v3/api-docs). Regenerate it after a
 * backend DTO changes; the type checker then points at every place that breaks.
 *
 * The generator marks every field as required because springdoc does not say
 * which ones can be null. The overrides below put `null` back on fields the
 * backend really leaves empty, so the compiler makes us handle those cases.
 */
import type { components } from "./schema";

type Schemas = components["schemas"];

/** Makes the listed keys nullable, leaving the rest of T as it is. */
type WithNullable<T, K extends keyof T> = Omit<T, K> & { [P in K]: T[P] | null };

// ---- Catalog --------------------------------------------------------------

export type Product = WithNullable<
    Schemas["Product"],
    "description" | "brand" | "imageUrl" | "averageRating"
>;
export type ProductCategory = Schemas["Product"]["category"];
export type ProductPayload = Schemas["ProductDTO"];

/** Spring Data page metadata (Page serialized VIA_DTO: items in `content`). */
export type PageMetadata = Schemas["PageMetadata"];
export interface Paged<T> {
    content: T[];
    page: PageMetadata;
}

// ---- Orders ---------------------------------------------------------------

export type OrderStatus = Schemas["OrderResponse"]["status"];
export type OrderItem = Schemas["Item"];
export type ShippingAddress = WithNullable<Schemas["ShippingAddressResponse"], "line2">;
export type Order = Omit<
    WithNullable<
        Schemas["OrderResponse"],
        "paidAt" | "shippedAt" | "deliveredAt" | "cancelledAt" | "couponCode" | "razorpayOrderId"
    >,
    "shippingAddress"
> & { shippingAddress: ShippingAddress | null };
export type PlaceOrderRequest = Schemas["PlaceOrderRequest"];

// ---- Cart, wishlist, notifications ----------------------------------------

export type CartItem = Schemas["CartItemResponse"];
export type WishlistEntry = Schemas["Wishlist"];
export type Notification = Schemas["Notification"];

// ---- Account --------------------------------------------------------------

export type Address = WithNullable<Schemas["AddressResponse"], "line2">;
export type AddressRequest = Schemas["AddressRequest"];
export type Profile = Schemas["ProfileResponse"];
export type PasswordChangeRequest = Schemas["PasswordChangeRequest"];

// ---- Reviews --------------------------------------------------------------

export type Review = WithNullable<Schemas["ReviewResponse"], "comment" | "updatedAt">;
export type ReviewRequest = Schemas["ReviewRequest"];
export interface MyReview {
    canReview: boolean;
    review: Review | null;
}

// ---- Coupons --------------------------------------------------------------

export type Coupon = WithNullable<Schemas["Coupon"], "minOrderAmount" | "maxUses" | "expiresAt">;
export type CouponType = Schemas["Coupon"]["type"];
export type CouponRequest = Schemas["CouponRequest"];
export type CouponValidation = Schemas["CouponValidationResponse"];

// ---- Admin ----------------------------------------------------------------

export type AdminStats = Schemas["AdminStatsResponse"];
export type DailyOrderStat = Schemas["DailyOrderStat"];

// ---- Endpoints the spec leaves untyped (they return a Map or ResponseEntity<?>)

/** GET /auth/me and POST /auth/login. */
export interface AuthUser {
    username: string;
    roles: string[] | string;
}

/** GET /payments/config. */
export interface PaymentConfig {
    keyId: string;
}

/** POST /payments/verify. */
export interface PaymentVerifyResult {
    status: string;
}

/** Body of the backend's ApiErrorResponse. */
export interface ApiError {
    message?: string;
    status?: number;
}
