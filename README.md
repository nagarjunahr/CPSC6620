# CPSC6620
CPSC 6620 – Project Part 3: Java Application

Project Overview:
This project implements a Java CRUD application that interacts with a relational database (from Part 2). The application manages customers, orders, pizzas, toppings, and inventory, allowing users to create, retrieve, update, and delete records. It also supports order subtypes (DineIn, Delivery, Pickup), applies discounts, updates inventory, and generates profitability reports.

Project Structure:
- cpsc4620/Menu.java – Main interface for the application. Handles user input and calls methods from DBNinja.
- cpsc4620/DBNinja.java – Implements all database operations including adding orders, managing customers, inventory, and generating reports.
- cpsc4620/DBConnector.java – Contains database connection parameters. Used by DBNinja for all SQL operations.
- Object Classes – Includes Order.java, Pizza.java, Customer.java, DeliveryOrder.java, PickupOrder.java, and DineInOrder.java. These should not be modified.
- sql/ – Contains the database scripts from Part 2 to create the tables and views needed by the application.

Features:
1. Add Orders: Add new orders with multiple pizzas and discounts. Updates topping inventory.
2. Manage Customers: View and add customer information.
3. View Orders: Display orders by status, date, or specific order.
4. View Order Details: Show pizzas, toppings, and discounts for selected orders.
5. Complete Orders: Mark orders as prepared, delivered, or picked up.
6. Inventory Management: View and restock toppings.
7. Reports: Generate ToppingPopularity, ProfitByPizza, and ProfitByOrderType reports.
8. Secure SQL: All user input queries use PreparedStatements to prevent SQL injection.

Setup Instructions:
1. Ensure Java 8 is installed.
2. Add MySQL JDBC Connector to your project libraries.
3. Update DBConnector.java with your database connection info (server, database name, username, password).
4. Run SQL scripts from the sql/ folder in MySQL Workbench to set up the database.
5. Compile and run the application using Menu.java.

Usage:
1. Launch the program via Menu.java.
2. Use the menu options to add orders, manage customers, view reports, and update inventory.
3. The program interacts with the database in real time, reflecting all changes immediately.

Notes:
- Do not modify method definitions in DBNinja.java or DBConnector.java.
- All input is validated minimally; avoid entering invalid data.
- Test SQL queries in MySQL Workbench before using them in Java.
- Use the autograder for Gradescope submissions to ensure correctness.

Author:
Nagarjuna
CPSC 6620
