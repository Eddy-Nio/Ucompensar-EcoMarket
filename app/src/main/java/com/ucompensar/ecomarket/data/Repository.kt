package com.ucompensar.ecomarket.data

/**
 * Repositorio en memoria que simula el backend REST descrito en la Actividad 1.
 * Se reemplazará por Retrofit + Room en la siguiente fase del proyecto.
 */
object Repository {

    const val IVA = 0.19

    val categories = listOf("Orgánicos", "Frutas", "Verduras", "Vegano", "Sin gluten", "Artesanías")

    val products = mutableListOf(
        Product(1, "Aguacate Hass orgánico", "Aguacate Hass de cultivo orgánico certificado, cosechado en Antioquia sin pesticidas.", 12500, 80, "Frutas", "Finca La Esperanza"),
        Product(2, "Café de altura tostado", "Café de origen Huila, tostado medio, 500 g. Comercio justo.", 38000, 45, "Orgánicos", "Cooperativa Cafetera del Sur"),
        Product(3, "Miel de abeja pura", "Miel multifloral sin procesar, frasco de vidrio de 350 g.", 24000, 30, "Orgánicos", "Apiarios Sierra Nevada"),
        Product(4, "Quinoa real premium", "Quinoa en grano de Nariño, sin gluten, bolsa de 500 g.", 19500, 60, "Sin gluten", "Finca La Esperanza"),
        Product(5, "Tomate cherry ecológico", "Tomate cherry de invernadero agroecológico, bandeja de 300 g.", 8900, 120, "Verduras", "Huerta Los Andes"),
        Product(6, "Huevos de campo libres", "Docena de huevos de gallinas en pastoreo libre.", 15800, 8, "Orgánicos", "Finca La Esperanza"),
        Product(7, "Jabón artesanal de lavanda", "Jabón vegano de lavanda y avena, barra de 120 g.", 11000, 70, "Vegano", "Taller Manos Verdes"),
        Product(8, "Mochila wayuu tejida", "Mochila tejida a mano por artesanas wayuu, diseño único.", 145000, 12, "Artesanías", "Asociación Wayuu Tejedoras"),
        Product(9, "Bebida de avena", "Bebida vegetal de avena sin azúcar añadida, 1 L.", 9800, 0, "Vegano", "Huerta Los Andes", Approval.PENDING)
    )

    val users = mutableListOf(
        User(1, "Camila Restrepo", "camila@ecomarket.co", "3001234567", Role.SELLER, business = "Finca La Esperanza"),
        User(2, "María G. Torres", "maria.torres@correo.com", "3109876543", Role.BUYER),
        User(3, "Jorge R. Medina", "jorge.medina@correo.com", "3205551122", Role.BUYER),
        User(4, "Ana L. Ríos", "ana.rios@correo.com", "3157778899", Role.BUYER, active = false),
        User(5, "Carlos P. Núñez", "carlos.nunez@correo.com", "3012223344", Role.BUYER),
        User(6, "Administrador EcoMarket", "admin@ecomarket.co", "3000000000", Role.ADMIN)
    )

    val cart = mutableListOf<CartItem>()

    val orders = mutableListOf(
        Order(1039, "Diego S.", listOf(CartItem(products[3], 1)), 19500, OrderStatus.DELIVERED, 3),
        Order(1040, "Lucía M.", listOf(CartItem(products[2], 3)), 72000, OrderStatus.DELIVERED, 4),
        Order(1041, "Jorge R.", listOf(CartItem(products[1], 1)), 38000, OrderStatus.SHIPPED, 5),
        Order(1042, "María G.", listOf(CartItem(products[0], 2)), 25000, OrderStatus.PENDING, 6)
    )

    /** Ventas base de los días previos (L..D) para que el reporte tenga histórico. */
    private val baseDailySales = intArrayOf(416000, 378000, 506000, 640000, 588000, 718000, 0)

    var currentUser: User = users[1]
        private set

    private var nextProductId = 100
    private var nextUserId = 100
    private var nextOrderId = 1043

    fun login(role: Role, email: String): User {
        val found = users.firstOrNull { it.role == role && it.email.equals(email, true) }
            ?: users.first { it.role == role }
        currentUser = found
        return found
    }

    fun logout() {
        cart.clear()
    }

    // ---------- Productos ----------
    fun approvedProducts(query: String = "", category: String? = null): List<Product> =
        products.filter {
            it.approval == Approval.APPROVED &&
                (category == null || it.category == category) &&
                (query.isBlank() || it.name.contains(query, true) || it.producer.contains(query, true))
        }

    /** Productos publicados por el negocio del vendedor autenticado. */
    fun productsOfCurrentSeller(): List<Product> =
        products.filter { it.producer == currentUser.business }

    fun productById(id: Int): Product? = products.firstOrNull { it.id == id }

    fun saveProduct(id: Int?, name: String, description: String, price: Int, stock: Int, category: String) {
        val existing = id?.let { productById(it) }
        if (existing != null) {
            existing.name = name
            existing.description = description
            existing.price = price
            existing.stock = stock
            existing.category = category
        } else {
            products.add(
                Product(nextProductId++, name, description, price, stock, category, currentUser.business.ifBlank { currentUser.name }, Approval.PENDING)
            )
        }
    }

    fun deleteProduct(id: Int) {
        products.removeAll { it.id == id }
        cart.removeAll { it.product.id == id }
    }

    // ---------- Usuarios ----------
    fun saveUser(id: Int?, name: String, email: String, phone: String, role: Role, active: Boolean) {
        val existing = id?.let { i -> users.firstOrNull { it.id == i } }
        if (existing != null) {
            existing.name = name
            existing.email = email
            existing.phone = phone
            existing.role = role
            existing.active = active
        } else {
            users.add(User(nextUserId++, name, email, phone, role, active))
        }
    }

    fun deleteUser(id: Int) {
        users.removeAll { it.id == id }
    }

    fun updateProfile(name: String, phone: String) {
        currentUser.name = name
        currentUser.phone = phone
    }

    // ---------- Carrito ----------
    fun addToCart(product: Product, quantity: Int = 1) {
        val line = cart.firstOrNull { it.product.id == product.id }
        if (line != null) {
            line.quantity = (line.quantity + quantity).coerceAtMost(product.stock.coerceAtLeast(1))
        } else {
            cart.add(CartItem(product, quantity.coerceAtMost(product.stock.coerceAtLeast(1))))
        }
    }

    fun cartSubtotal(): Int = cart.sumOf { it.subtotal }
    fun cartTax(): Int = (cartSubtotal() * IVA).toInt()
    fun cartTotal(): Int = cartSubtotal() + cartTax()

    fun checkout(): Order {
        val items = cart.map { CartItem(it.product, it.quantity) }
        val total = cartTotal()
        items.forEach { it.product.stock = (it.product.stock - it.quantity).coerceAtLeast(0) }
        val order = Order(nextOrderId++, currentUser.name, items, total, OrderStatus.PENDING, 6)
        orders.add(0, order)
        cart.clear()
        return order
    }

    // ---------- Reportes ----------
    fun dailySales(): List<Int> {
        val result = baseDailySales.copyOf()
        orders.filter { it.id >= 1043 }.forEach { result[it.dayIndex] += it.total }
        result[6] += orders.filter { it.id < 1043 && it.dayIndex == 6 }.sumOf { it.total }
        return result.toList()
    }

    fun totalSales(): Int = dailySales().sum()

    fun salesByCategory(): List<Pair<String, Int>> =
        orders.flatMap { it.items }
            .groupBy { it.product.category }
            .map { (cat, lines) -> cat to lines.sumOf { it.subtotal } }
            .sortedByDescending { it.second }

    fun topProducts(): List<Pair<String, Int>> =
        orders.flatMap { it.items }
            .groupBy { it.product.name }
            .map { (name, lines) -> name to lines.sumOf { it.quantity } }
            .sortedByDescending { it.second }
            .take(5)
}
