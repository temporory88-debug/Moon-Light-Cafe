package com.example

data class MenuItem(
    val name: String,
    val category: String,
    val price: Int,
    val emoji: String,
    val description: String,
    val isVeg: Boolean,
    val isBestseller: Boolean = false,
    val badge: String? = null
)

data class Category(
    val id: String,
    val name: String,
    val emoji: String
)

object MenuData {
    val categories = listOf(
        Category("bestseller", "Best Sellers", "⭐"),
        Category("coffee", "Coffee & Tea", "☕"),
        Category("refreshers", "Refreshers", "🍹"),
        Category("bubbletea", "Bubble Tea", "🧋"),
        Category("lassi", "Lassi", "🥛"),
        Category("momo", "Momo", "🥟"),
        Category("noodles", "Noodles", "🍜"),
        Category("snacks", "Snacks", "🍟"),
        Category("pizza", "Pizza", "🍕"),
        Category("burger", "Burger", "🍔"),
        Category("sandwich", "Sandwich", "🥪"),
        Category("pasta", "Pasta", "🍝"),
        Category("dessert", "Desserts", "🍰"),
        Category("others", "Others", "🥤")
    )

    val menuItems = listOf(
        // Best Sellers
        MenuItem(
            name = "Moon Light Special Coffee",
            category = "bestseller",
            price = 250,
            emoji = "☕",
            description = "Our rich, premium signature house special coffee blend, finished with cream.",
            isVeg = true,
            isBestseller = true,
            badge = "#1"
        ),
        MenuItem(
            name = "Moon Light Special Mojito",
            category = "bestseller",
            price = 320,
            emoji = "🍹",
            description = "A refreshing craft drink infused with fresh mint, lime, and our custom secret flavor.",
            isVeg = true,
            isBestseller = true,
            badge = "#2"
        ),
        MenuItem(
            name = "Moon Light Special Burger",
            category = "bestseller",
            price = 300,
            emoji = "🍔",
            description = "Loaded premium chef's special buff/chicken burger with double cheese and house sauce.",
            isVeg = false,
            isBestseller = true,
            badge = "#3"
        ),
        MenuItem(
            name = "Moon Light Special Pizza",
            category = "bestseller",
            price = 500,
            emoji = "🍕",
            description = "Loaded pizza topped with mixed toppings, melting mozzarella and rustic herbs.",
            isVeg = false,
            isBestseller = true,
            badge = "#4"
        ),

        // Coffee
        MenuItem(
            name = "Espresso",
            category = "coffee",
            price = 100,
            emoji = "☕",
            description = "Bold essence of espresso brewed from fresh medium-dark roast beans.",
            isVeg = true
        ),
        MenuItem(
            name = "Cafe Lungo",
            category = "coffee",
            price = 220,
            emoji = "☕",
            description = "A longer pulled, lighter bodied double espresso with smooth profile.",
            isVeg = true
        ),
        MenuItem(
            name = "Espresso Affogato",
            category = "coffee",
            price = 200,
            emoji = "☕",
            description = "Rich double shot espresso poured over a scoop of premium vanilla ice cream.",
            isVeg = true
        ),
        MenuItem(
            name = "Espresso Con Panna",
            category = "coffee",
            price = 150,
            emoji = "☕",
            description = "Espresso shot topped with a generous dollop of whipped cream.",
            isVeg = true
        ),
        MenuItem(
            name = "Americano Lite",
            category = "coffee",
            price = 90,
            emoji = "☕",
            description = "Espresso shot diluted with hot water for a crisp, clean cup.",
            isVeg = true
        ),
        MenuItem(
            name = "Americano Strong",
            category = "coffee",
            price = 220,
            emoji = "☕",
            description = "A double espresso Americano for maximum focus and rich taste.",
            isVeg = true
        ),
        MenuItem(
            name = "Cappuccino",
            category = "coffee",
            price = 150,
            emoji = "☕",
            description = "Classic balance of rich espresso, steamed milk, and velvety thick foam.",
            isVeg = true
        ),
        MenuItem(
            name = "Cafe Latte",
            category = "coffee",
            price = 150,
            emoji = "🥛",
            description = "Smooth espresso blended with sweet steamed milk and a thin layer of foam.",
            isVeg = true
        ),
        MenuItem(
            name = "Cafe Mocha",
            category = "coffee",
            price = 200,
            emoji = "☕",
            description = "Rich espresso, smooth steamed milk, and rich sweetened cocoa dark chocolate.",
            isVeg = true
        ),
        MenuItem(
            name = "Latte Macchiato",
            category = "coffee",
            price = 200,
            emoji = "☕",
            description = "Layered steamed milk marked with a bold double shot of hot espresso.",
            isVeg = true
        ),
        MenuItem(
            name = "Mocha Madness",
            category = "coffee",
            price = 200,
            emoji = "☕",
            description = "Extremely rich mocha with double chocolate and dynamic cocoa dust.",
            isVeg = true
        ),
        MenuItem(
            name = "Hot Chocolate",
            category = "coffee",
            price = 200,
            emoji = "🍫",
            description = "Velvety steamed whole milk mixed with rich Belgian cocoa chocolate.",
            isVeg = true
        ),
        MenuItem(
            name = "Iced Americano",
            category = "coffee",
            price = 150,
            emoji = "🧊",
            description = "Chilled espresso served over ice cube sheets with cold water.",
            isVeg = true
        ),
        MenuItem(
            name = "Iced Cappuccino",
            category = "coffee",
            price = 180,
            emoji = "🧊",
            description = "Chilled espresso with cold frothed milk, served ice-cold.",
            isVeg = true
        ),
        MenuItem(
            name = "Iced Latte",
            category = "coffee",
            price = 180,
            emoji = "🧊",
            description = "Cold milk marked with rich espresso and poured over clear ice.",
            isVeg = true
        ),
        MenuItem(
            name = "Black Tea",
            category = "coffee",
            price = 40,
            emoji = "🫖",
            description = "Traditional Nepalese premium black CTC tea leaf brew.",
            isVeg = true
        ),
        MenuItem(
            name = "Lemon Tea",
            category = "coffee",
            price = 50,
            emoji = "🍋",
            description = "Refreshing local black tea infused with fresh citrus lemon juice.",
            isVeg = true
        ),
        MenuItem(
            name = "Green Tea",
            category = "coffee",
            price = 70,
            emoji = "🍵",
            description = "Soothing, organic antioxidant-rich premium green herbal tea leaves.",
            isVeg = true
        ),
        MenuItem(
            name = "Hot Lemon Honey Ginger",
            category = "coffee",
            price = 220,
            emoji = "🍯",
            description = "A healing local organic blend of hot lime, fresh crushed ginger, and golden honey.",
            isVeg = true
        ),
        MenuItem(
            name = "Milk Tea",
            category = "coffee",
            price = 40,
            emoji = "🥛",
            description = "Classic Nepalese CTC milk tea, sweet and energizing.",
            isVeg = true
        ),

        // Refreshers
        MenuItem(
            name = "Blended Lemonade",
            category = "refreshers",
            price = 200,
            emoji = "🍋",
            description = "Ice-blended sweet and tangy classic lemonade.",
            isVeg = true
        ),
        MenuItem(
            name = "Blended Mint Lemonade",
            category = "refreshers",
            price = 220,
            emoji = "🍋",
            description = "Ice-blended lemonade infused with fresh vibrant mint leaves.",
            isVeg = true
        ),
        MenuItem(
            name = "Iced Strawberry",
            category = "refreshers",
            price = 240,
            emoji = "🍓",
            description = "Sweet and icy strawberry fruit infusion, perfectly chilled.",
            isVeg = true
        ),
        MenuItem(
            name = "Iced Orange",
            category = "refreshers",
            price = 240,
            emoji = "🍊",
            description = "Fresh pressed sweet orange squash served ice cold.",
            isVeg = true
        ),
        MenuItem(
            name = "Virgin Mojito",
            category = "refreshers",
            price = 240,
            emoji = "🍹",
            description = "Refreshing local combination of lime, fresh mint, sugar syrup, and soda.",
            isVeg = true,
            badge = "Popular"
        ),
        MenuItem(
            name = "Iced Strawberry Mojito",
            category = "refreshers",
            price = 290,
            emoji = "🍓",
            description = "Classic mint mojito with sweet crushed strawberries.",
            isVeg = true
        ),
        MenuItem(
            name = "Iced Blueberry Mojito",
            category = "refreshers",
            price = 290,
            emoji = "🫐",
            description = "Cool mojito base paired with rich, premium blueberry syrup.",
            isVeg = true
        ),
        MenuItem(
            name = "Iced Mango Mojito",
            category = "refreshers",
            price = 290,
            emoji = "🥭",
            description = "Golden sweet mango nectar twisted with mint, lime, and soda.",
            isVeg = true
        ),
        MenuItem(
            name = "Iced Orange Mojito",
            category = "refreshers",
            price = 290,
            emoji = "🍊",
            description = "Zesty fresh citrus orange syrup with sparkling soda and cool mint.",
            isVeg = true
        ),

        // Bubble Tea
        MenuItem(
            name = "Blueberry Bubble Tea",
            category = "bubbletea",
            price = 230,
            emoji = "🫐",
            description = "Creamy taro-blueberry milk tea with sweet chewy tapioca pearls.",
            isVeg = true
        ),
        MenuItem(
            name = "Strawberry Bubble Tea",
            category = "bubbletea",
            price = 230,
            emoji = "🍓",
            description = "Sweet strawberry infused cold tea base with popping fruit boba.",
            isVeg = true
        ),
        MenuItem(
            name = "Chocolate Bubble Tea",
            category = "bubbletea",
            price = 330,
            emoji = "🍫",
            description = "Thick premium chocolate milk tea with soft tapioca pearls.",
            isVeg = true
        ),

        // Lassi
        MenuItem(
            name = "Mango Lassi",
            category = "lassi",
            price = 200,
            emoji = "🥭",
            description = "Thick traditional yogurt shake blended with sweet mango pulp.",
            isVeg = true,
            badge = "Popular"
        ),
        MenuItem(
            name = "Strawberry Lassi",
            category = "lassi",
            price = 200,
            emoji = "🍓",
            description = "Creamy yogurt drink blended with fresh sweet strawberries.",
            isVeg = true
        ),
        MenuItem(
            name = "Orange Lassi",
            category = "lassi",
            price = 200,
            emoji = "🍊",
            description = "A citrus twist of yogurt and fresh local orange puree.",
            isVeg = true
        ),
        MenuItem(
            name = "Banana Lassi",
            category = "lassi",
            price = 200,
            emoji = "🍌",
            description = "Smooth, energy-boosting lassi with sweet local bananas.",
            isVeg = true
        ),
        MenuItem(
            name = "Sweet Lassi",
            category = "lassi",
            price = 140,
            emoji = "🥛",
            description = "Chilled, sweet, classic churned yogurt lassi with a splash of rose water.",
            isVeg = true
        ),
        MenuItem(
            name = "Plain Lassi",
            category = "lassi",
            price = 140,
            emoji = "🥛",
            description = "Fresh, unsweetened, cooling traditional home-style churned yogurt lassi.",
            isVeg = true
        ),

        // Momo
        MenuItem(
            name = "Veg Momo",
            category = "momo",
            price = 100,
            emoji = "🥟",
            description = "Steamed spiced vegetable dumplings, served with standard spicy tomato pickle achar.",
            isVeg = true
        ),
        MenuItem(
            name = "Chicken Momo",
            category = "momo",
            price = 120,
            emoji = "🥟",
            description = "Classic Nepalese chicken momo dumplings, minced and perfectly spiced, served with achar.",
            isVeg = false,
            badge = "Popular"
        ),
        MenuItem(
            name = "Steam Buffalo Momo",
            category = "momo",
            price = 120,
            emoji = "🥟",
            description = "Freshly steamed buff meat momo dumplings, rich in flavor, served with cold sesame peanut soup.",
            isVeg = false
        ),

        // Noodles
        MenuItem(
            name = "Chicken Chowmein",
            category = "noodles",
            price = 150,
            emoji = "🍜",
            description = "Stir-fried noodles with chicken, fresh shredded vegetables, and aromatic Nepalese seasoning.",
            isVeg = false
        ),
        MenuItem(
            name = "Veg Chowmein",
            category = "noodles",
            price = 130,
            emoji = "🍜",
            description = "Fried noodles with crisp bell peppers, shredded cabbage, carrots, and sweet soy sauce.",
            isVeg = true
        ),
        MenuItem(
            name = "Buff Thukpa",
            category = "noodles",
            price = 160,
            emoji = "🍜",
            description = "Warm authentic Tibetan spiced noodle soup with buffalo meat slices and coriander.",
            isVeg = false
        ),

        // Snacks
        MenuItem(
            name = "French Fries",
            category = "snacks",
            price = 120,
            emoji = "🍟",
            description = "Salty crispy golden-fried potato fingers, served warm with signature ketchup.",
            isVeg = true
        ),
        MenuItem(
            name = "Chicken Chilli Sausage",
            category = "snacks",
            price = 150,
            emoji = "🌭",
            description = "Sautéed chicken sausage links tossed in hot garlic, capsicum and sweet chilli glaze.",
            isVeg = false
        ),
        MenuItem(
            name = "Seasoned Potato Wedges",
            category = "snacks",
            price = 140,
            emoji = "🍟",
            description = "Thick skin-on seasoned chunk potato wedges, baked and fried until extremely crispy.",
            isVeg = true
        ),

        // Pizza
        MenuItem(
            name = "Classic Margherita Pizza",
            category = "pizza",
            price = 350,
            emoji = "🍕",
            description = "Simple elegance with rich tomato pomodoro sauce, fresh basil oil, and double melting mozzarella.",
            isVeg = true
        ),
        MenuItem(
            name = "Chicken Tikka Pizza",
            category = "pizza",
            price = 450,
            emoji = "🍕",
            description = "Bold pizza topped with oven charred barbecue chicken tikka chunks and bell peppers.",
            isVeg = false
        ),

        // Burger
        MenuItem(
            name = "Crispy Veg Burger",
            category = "burger",
            price = 180,
            emoji = "🍔",
            description = "Crispy handcrafted vegetable patty inside a toasted brioche bun with crispy lettuce and burger mayo.",
            isVeg = true
        ),
        MenuItem(
            name = "Double Chicken Cheese Burger",
            category = "burger",
            price = 220,
            emoji = "🍔",
            description = "Juicy grilled chicken patty topped with thick melting American cheddar cheese slice, pickles and onions.",
            isVeg = false
        ),

        // Sandwich
        MenuItem(
            name = "Toasted Grilled Cheese",
            category = "sandwich",
            price = 150,
            emoji = "🥪",
            description = "Hot griddled toasted sound sourdough bread loaded with melting mozzarella and golden cheddar mix.",
            isVeg = true
        ),
        MenuItem(
            name = "Moon Light Club Sandwich",
            category = "sandwich",
            price = 220,
            emoji = "🥪",
            description = "Premium triple-decker sandwich stuffed with egg omelet, grilled chicken breast, fresh crisp tomatoes, and herb mayo.",
            isVeg = false
        ),

        // Pasta
        MenuItem(
            name = "Creamy White Sauce Pasta",
            category = "pasta",
            price = 250,
            emoji = "🍝",
            description = "Penne pasta tossed in smooth, garlic-infused extra cheesy Alfredo white parmesan sauce.",
            isVeg = true
        ),
        MenuItem(
            name = "Spicy Red Sauce Pasta",
            category = "pasta",
            price = 230,
            emoji = "🍝",
            description = "Penne pasta wok-tossed in robust spicy tarragon herb tomato garlic arrabbiata sauce.",
            isVeg = true
        ),

        // Desserts
        MenuItem(
            name = "Sizzling Chocolate Brownie",
            category = "dessert",
            price = 280,
            emoji = "🍰",
            description = "Warm dark chocolate fudge brownie served on a piping hot sizzler plate with cold vanilla bean ice cream scoop.",
            isVeg = true
        ),
        MenuItem(
            name = "Oozo Choco Lava Cake",
            category = "dessert",
            price = 180,
            emoji = "🍰",
            description = "Freshly baked warm cocoa cake with an oozing rich core of melting fluid dark chocolate.",
            isVeg = true
        ),

        // Others
        MenuItem(
            name = "Processed Mineral Water",
            category = "others",
            price = 30,
            emoji = "🥤",
            description = "Chilled, multi-stage purified local filtered drinking mineral water bottled at source.",
            isVeg = true
        ),
        MenuItem(
            name = "Coca Cola / Sprite / Fanta",
            category = "others",
            price = 60,
            emoji = "🥤",
            description = "Ice-cold refreshing carbonated soda can of your choice.",
            isVeg = true
        )
    )
}
