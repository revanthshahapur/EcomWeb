// ========== CART STORAGE HELPERS ==========
function getCart() {
    return JSON.parse(localStorage.getItem('cart')) || [];
}

function saveCart(cart) {
    localStorage.setItem('cart', JSON.stringify(cart));
    updateCartDisplay();
    updateCartModal();
}

// ========== ADD TO CART ==========
function addToCart(product) {
    let cart = getCart();
    const existingItemIndex = cart.findIndex(item =>
        item.id === product.id &&
        item.grade === product.grade &&
        item.shape === product.shape
    );

    if (existingItemIndex > -1) {
        cart[existingItemIndex].quantity += 1;
    } else {
        cart.push({
            id: product.id,
            name: product.name,
            price: product.price,
            image: product.image,
            grade: product.grade,
            shape: product.shape,
            quantity: 1
        });
    }

    saveCart(cart);
    showAddToCartPopup(product.name);
}

// ========== REMOVE ITEM ==========
function removeFromCart(productId, grade, shape) {
    let cart = getCart();
    cart = cart.filter(item => !(item.id === productId && item.grade === grade && item.shape === shape));
    saveCart(cart);
}

// ========== UI UPDATE - DROPDOWN ==========
function updateCartDisplay() {
    const cart = getCart();
    const cartCount = cart.reduce((total, item) => total + item.quantity, 0);
    const cartSubtotal = cart.reduce((total, item) => total + (item.price * item.quantity), 0);

    const cartCountEl = document.getElementById('cart-count');
    const cartItemCountEl = document.getElementById('cart-item-count');
    const cartSubtotalEl = document.getElementById('cart-subtotal');
    if (cartCountEl) cartCountEl.textContent = cartCount;
    if (cartItemCountEl) cartItemCountEl.textContent = cartCount;
    if (cartSubtotalEl) cartSubtotalEl.textContent = cartSubtotal.toFixed(2);

    const cartDropdownList = document.getElementById('cart-dropdown-list');
    if (cartDropdownList) {
        cartDropdownList.innerHTML = '';
        if (cart.length === 0) {
            cartDropdownList.innerHTML = '<p style="text-align: center; color: #999; padding: 20px;">Your cart is empty</p>';
        } else {
            cart.forEach(item => {
                const productWidget = document.createElement('div');
                productWidget.className = 'product-widget';
                productWidget.innerHTML = `
                    <div class="product-img">
                        <img src="${item.image}" alt="${item.name}">
                    </div>
                    <div class="product-body">
                        <h3 class="product-name">${item.name}</h3>
                        <h4 class="product-price"><span class="qty">${item.quantity}x</span> $${(item.price * item.quantity).toFixed(2)}</h4>
                        <p><small>Grade: ${item.grade}, Shape: ${item.shape}</small></p>
                    </div>
                    <button class="delete" onclick="removeFromCart('${item.id}', '${item.grade}', '${item.shape}')"><i class="fa fa-close"></i></button>
                `;
                cartDropdownList.appendChild(productWidget);
            });
        }
    }
}

// ========== UI UPDATE - MODAL ==========
function updateCartModal() {
    const cart = getCart();
    const container = document.getElementById('modal-cart-items-container');
    const subtotalElem = document.getElementById('modal-subtotal');
    const taxElem = document.getElementById('modal-tax');
    const totalElem = document.getElementById('modal-total');

    if (!container || !subtotalElem || !taxElem || !totalElem) return;

    if (cart.length === 0) {
        container.innerHTML = `
            <div class="text-center" style="padding: 40px;">
                <i class="fa fa-shopping-cart" style="font-size: 48px; color: #ccc; margin-bottom: 20px;"></i>
                <h4>Your cart is empty</h4>
                <p class="text-muted">Add some products to get started!</p>
            </div>
        `;
        subtotalElem.textContent = "$0.00";
        taxElem.textContent = "$0.00";
        totalElem.textContent = "$0.00";
        return;
    }

    container.innerHTML = '';
    let subtotal = 0;
    cart.forEach(item => {
        const itemTotal = item.price * item.quantity;
        subtotal += itemTotal;

        const cartItem = document.createElement('div');
        cartItem.className = 'cart-item';
        cartItem.innerHTML = `
            <div class="row align-items-center mb-3">
                <div class="col-md-2">
                    <img src="${item.image}" alt="${item.name}" class="img-fluid rounded">
                </div>
                <div class="col-md-4">
                    <h6>${item.name}</h6>
                    <small class="text-muted">Grade: ${item.grade} | Shape: ${item.shape}</small>
                </div>
                <div class="col-md-2"><strong>$${item.price.toFixed(2)}</strong></div>
                <div class="col-md-3">
                    <div class="quantity-controls d-flex align-items-center">
                        <button class="btn btn-sm btn-outline-secondary" onclick="updateModalCartQuantity('${item.id}', '${item.grade}', '${item.shape}', ${item.quantity - 1})">-</button>
                        <input type="number" class="form-control form-control-sm mx-2 text-center" value="${item.quantity}" min="1" style="width: 60px;" onchange="updateModalCartQuantity('${item.id}', '${item.grade}', '${item.shape}', this.value)">
                        <button class="btn btn-sm btn-outline-secondary" onclick="updateModalCartQuantity('${item.id}', '${item.grade}', '${item.shape}', ${item.quantity + 1})">+</button>
                    </div>
                </div>
                <div class="col-md-1 text-right">
                    <button class="btn btn-sm btn-outline-danger" onclick="removeModalCartItem('${item.id}', '${item.grade}', '${item.shape}')">
                        <i class="fa fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
        container.appendChild(cartItem);
    });

    const tax = subtotal * 0.08;
    const shipping = 50.00;
    const total = subtotal + shipping + tax;

    subtotalElem.textContent = `$${subtotal.toFixed(2)}`;
    taxElem.textContent = `$${tax.toFixed(2)}`;
    totalElem.textContent = `$${total.toFixed(2)}`;
}

// ========== QUANTITY & CHECKOUT ==========
function updateModalCartQuantity(productId, grade, shape, newQuantity) {
    if (newQuantity < 1) {
        removeModalCartItem(productId, grade, shape);
        return;
    }

    let cart = getCart();
    const itemIndex = cart.findIndex(item =>
        item.id === productId && item.grade === grade && item.shape === shape
    );

    if (itemIndex > -1) {
        cart[itemIndex].quantity = parseInt(newQuantity);
        saveCart(cart);
    }
}

function removeModalCartItem(productId, grade, shape) {
    removeFromCart(productId, grade, shape);
}

function proceedToCheckout() {
    const cart = getCart();
    if (cart.length === 0) {
        alert("Your cart is empty. Please add some items before checkout.");
        return;
    }
    window.location.href = "checkout.html";
}

// ========== POPUP + UI INIT ==========
function showAddToCartPopup(productName) {
    const popup = document.createElement('div');
    popup.className = 'added-to-cart-popup show';
    popup.innerHTML = `<i class="fa fa-check-circle"></i> ${productName} added to cart!`;
    document.body.appendChild(popup);
    setTimeout(() => {
        popup.classList.remove('show');
        setTimeout(() => {
            document.body.removeChild(popup);
        }, 500);
    }, 3000);
}

function handleAddToCart(button) {
    const productElement = button.closest('.product');
    const id = productElement.dataset.productId;
    const name = productElement.dataset.productName;
    const price = parseFloat(productElement.dataset.productPrice);
    const image = productElement.dataset.productImage;
    const grade = productElement.querySelector('.grade-selector')?.value || '';
    const shape = productElement.querySelector('.shape-selector')?.value || '';
    const product = { id, name, price, image, grade, shape };
    addToCart(product);
}

// ========== MODAL HANDLERS ==========
function toggleCartDropdown(event) {
    event.preventDefault();
    const dropdown = document.querySelector('.dropdown');
    dropdown.classList.toggle('open');
}

function openCartModal() {
    $('#cartModal').modal('show');
    updateCartModal();
}

// ========== INIT ==========
document.addEventListener('DOMContentLoaded', function () {
    updateCartDisplay();
    updateCartModal();
});

document.addEventListener('click', function (event) {
    const dropdown = document.querySelector('.dropdown');
    if (dropdown && !dropdown.contains(event.target)) {
        dropdown.classList.remove('open');
    }
});

window.openCartModal = openCartModal;
window.proceedToCheckout = proceedToCheckout;
window.toggleCartDropdown = toggleCartDropdown;
