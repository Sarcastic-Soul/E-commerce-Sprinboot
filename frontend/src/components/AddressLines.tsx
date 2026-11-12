import type { ShippingAddress } from "../api/types";

/** The fields shared by Address and an order's ShippingAddress. */
export type AddressLike = Pick<
    ShippingAddress,
    "fullName" | "line1" | "line2" | "city" | "state" | "postalCode" | "country" | "phone"
>;

/** Plain postal-style rendering of an Address or an order's shippingAddress. */
export interface AddressLinesProps {
    address?: AddressLike | null;
    className?: string;
}

export default function AddressLines({ address, className = "" }: AddressLinesProps) {
    if (!address) return null;
    return (
        <address className={`not-italic leading-snug ${className}`}>
            <span className="block font-black">{address.fullName}</span>
            <span className="block">{address.line1}</span>
            {address.line2 && <span className="block">{address.line2}</span>}
            <span className="block">
                {address.city}, {address.state} {address.postalCode}
            </span>
            {address.country && <span className="block">{address.country}</span>}
            <span className="block opacity-80">Phone: {address.phone}</span>
        </address>
    );
}
