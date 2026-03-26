import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi } from "vitest";
import ProductCard from "./ProductCard";

// 1. Mock react-router-dom
vi.mock("react-router-dom", () => ({
    useNavigate: () => vi.fn(),
}));

// 2. Mock AuthContext
vi.mock("../context/AuthContext", () => ({
    useAuth: () => ({ user: null }),
}));

// 3. Mock ThemeContext
vi.mock("../context/ThemeContext", () => ({
    useTheme: () => ({ darkMode: false }),
}));

// 4. Mock CartContext
vi.mock("../context/CartContext.jsx", () => ({
    useCart: () => ({ setCartItemCount: vi.fn() }),
}));

describe("ProductCard Component", () => {
    const mockProduct = {
        id: 1,
        name: "NeoBrutal Keyboard",
        price: 120.5,
        imageUrl: "http://example.com/keyboard.jpg",
        category: "ELECTRONICS",
    };

    it("renders product information correctly", () => {
        // Render the component with our fake product
        render(<ProductCard product={mockProduct} onClick={() => {}} />);

        // Check if the name, price, and category are on the screen
        expect(screen.getByText("NeoBrutal Keyboard")).toBeInTheDocument();
        expect(screen.getByText("$120.50")).toBeInTheDocument();
        expect(screen.getByText("ELECTRONICS")).toBeInTheDocument();
    });

    it("calls the onClick handler when clicked", async () => {
        const handleClick = vi.fn();
        const user = userEvent.setup();

        render(<ProductCard product={mockProduct} onClick={handleClick} />);

        const cardElement = screen.getByText("NeoBrutal Keyboard");
        await user.click(cardElement);

        expect(handleClick).toHaveBeenCalledTimes(1);
    });
});
