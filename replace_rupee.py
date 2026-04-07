import os
import re

directories = ['frontend/src']

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content

    # In JSX, things like `${product.price}` might be preceded by ">", "> ", or newlines
    # We want to replace $ followed by { where { contains variables like price, total, etc.
    # Pattern: `$` followed by `{` then some text then `}`
    # To avoid matching literal `${` inside template strings which are part of JS interpolation,
    # we can just find any literal "$" that is used as a currency symbol.
    
    # We can replace literal `$${` inside template literals
    content = content.replace('`$${', '`₹${')

    # Replace `$` followed by `{` where it's a price. Let's look for known patterns:
    # 1. ${item.price.toFixed(2)}
    content = content.replace('${item.price.toFixed(2)}', '₹{item.price.toFixed(2)}')
    
    # 2. ${cartTotal.toFixed(2)}
    content = content.replace('${cartTotal.toFixed(2)}', '₹{cartTotal.toFixed(2)}')

    # 3. ${product.price.toFixed(2)}
    content = content.replace('${product.price.toFixed(2)}', '₹{product.price.toFixed(2)}')
    
    # 4. ${order.totalAmount.toFixed(2)}
    content = content.replace('${order.totalAmount.toFixed(2)}', '₹{order.totalAmount.toFixed(2)}')
    
    # 5. ${(item.priceAtPurchase * item.quantity).toFixed(2)}
    content = content.replace('${(item.priceAtPurchase * item.quantity).toFixed(2)}', '₹{(item.priceAtPurchase * item.quantity).toFixed(2)}')

    # 6. Min $ and Max $
    content = content.replace('Min $', 'Min ₹')
    content = content.replace('Max $', 'Max ₹')
    
    # AddProductPage and UpdateProductPage "Price ($)"
    content = content.replace('Price ($)', 'Price (₹)')

    if original_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {filepath}")

for root, _, files in os.walk(directories[0]):
    for file in files:
        if file.endswith('.jsx') or file.endswith('.js'):
            process_file(os.path.join(root, file))

