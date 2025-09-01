import java.util.*;
import java.sql.*;

abstract class vehicle {
    private String vehicleId;
    private String brand;
    private String model;
    public String vehicleType;
    private double basePricePerDay;
    private boolean isAvailable;

    public String getVehicleId(){ 
    	return vehicleId; }
    public String getBrand() { 
    	return brand; }
    public String getModel() {
    	return model; }
    public double calculatePrice(int rentalDays) { 
    	return basePricePerDay * rentalDays; }
    public boolean isAvailable() { 
    	return isAvailable; }
    public void rent() { 
    	isAvailable = false; }
    public void returnVehicle() {
    	isAvailable = true; }

    public void setVehicleId(String vehicleId) {
    	this.vehicleId = vehicleId; }
    public void setBrand(String brand) { 
    	this.brand = brand; }
    public void setModel(String model) { 
    	this.model = model; }
    public void setAvailable(boolean a) {
    	this.isAvailable = a; }
    public void setBasePricePerDay(double price) { 
    	this.basePricePerDay = price; }
}

class Car extends vehicle {
    public Car(String vehicleType, String vehicleId, String brand, String model, double price) {
        super.vehicleType = vehicleType;
        super.setVehicleId(vehicleId);
        super.setBrand(brand);
        super.setModel(model);
        super.setBasePricePerDay(price);
        super.setAvailable(true);
    }
}

class Bike extends vehicle {
    public Bike(String vehicleType, String vehicleId, String brand, String model, double price) {
        super.vehicleType = vehicleType;
        super.setVehicleId(vehicleId);
        super.setBrand(brand);
        super.setModel(model);
        super.setBasePricePerDay(price);
        super.setAvailable(true);
    }
}

class customer {
    private String customerId;
    private String customerName;
    private String phoneNumber;
    private String password;

    public customer(String customerId, String customerName, String phoneNumber, String password) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    public String getCustomerId() { 
    	return customerId; }
    public String getCustomerName() {
    	return customerName; }
    public String getPhoneNumber() { 
    	return phoneNumber; }
    public String getPassword() {
    	return password; }
    public boolean validatePassword(String inputPassword) {
        return password.equals(inputPassword);
    }
}

class CreateVehicle {
    public static vehicle create(String vehicleType, String vehicleId, String brand, String model, double price) {
        if (vehicleType.equalsIgnoreCase("car")) {
            return new Car("Car", vehicleId, brand, model, price);
        } else if (vehicleType.equalsIgnoreCase("bike")) {
            return new Bike("Bike", vehicleId, brand, model, price);
        } else {
            throw new IllegalArgumentException("Invalid vehicle type");
        }
    }
}

class Rental {
    private vehicle vehicle;
    private customer customer;
    private int days;

    public Rental(vehicle vehicle, customer customer, int days) {
        this.vehicle = vehicle;
        this.customer = customer;
        this.days = days;
    }

    public vehicle getVehicle() {
    	return vehicle; }
    public customer getCustomer() { 
    	return customer; }
    public int getDays() { 
    	return days; }
}

class DBConnection {
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/vehicle_rental", "root", "Iswar@67890");
    }
}

class VehicleRentalSystem {
    private List<vehicle> vehicles = new ArrayList<>();
    private List<customer> customers = new ArrayList<>();
    private List<Rental> rentals = new ArrayList<>();

    public void addVehicle(vehicle v) {
        vehicles.add(v);
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            String sql = String.format(
                "INSERT INTO vehicles VALUES ('%s', '%s', '%s', '%s', %f, %b)",
                v.getVehicleId(), v.vehicleType, v.getBrand(), v.getModel(), v.calculatePrice(1), v.isAvailable()
            );
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("DB Error (Vehicle): " + e.getMessage());
        }
    }

    public void addCustomer(customer c) {
        customers.add(c);
        try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
            String sql = String.format(
                "INSERT INTO customers VALUES ('%s', '%s', '%s', '%s')",
                c.getCustomerId(), c.getCustomerName(), c.getPhoneNumber(), c.getPassword()
            );
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("DB Error (Customer): " + e.getMessage());
        }
    }

    public void rentVehicle(vehicle v, customer c, int days) {
        if (v.isAvailable()) {
            v.rent();
            rentals.add(new Rental(v, c, days));
            try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(String.format(
                    "INSERT INTO rentals (vehicleId, customerId, rentalDays, rentalDate) VALUES ('%s', '%s', %d, CURDATE())",
                    v.getVehicleId(), c.getCustomerId(), days
                ));
                stmt.executeUpdate(String.format(
                    "UPDATE vehicles SET isAvailable = false WHERE vehicleId = '%s'",
                    v.getVehicleId()
                ));
            } catch (SQLException e) {
                System.out.println("DB Error (Rent): " + e.getMessage());
            }
        } else {
            System.out.println("Vehicle not available");
        }
    }

    public void returnVehicle(vehicle v) {
        v.returnVehicle();
        Rental remove = null;
        for (Rental r : rentals) {
            if (r.getVehicle() == v) {
                remove = r;
                break;
            }
        }

        if (remove != null) {
            rentals.remove(remove);
            try (Connection conn = DBConnection.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(String.format(
                    "DELETE FROM rentals WHERE vehicleId = '%s' AND customerId = '%s'",
                    remove.getVehicle().getVehicleId(), remove.getCustomer().getCustomerId()
                ));
                stmt.executeUpdate(String.format(
                    "UPDATE vehicles SET isAvailable = true WHERE vehicleId = '%s'",
                    v.getVehicleId()
                ));
            } catch (SQLException e) {
                System.out.println("DB Error (Return): " + e.getMessage());
            }
        } else {
            System.out.println("Vehicle not rented");
        }
    }

    public void loadData() {
        try (Connection con = DBConnection.getConnection(); Statement stmt = con.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM customers");
            while (rs.next()) {
                customer c = new customer(
                    rs.getString("customerId"),
                    rs.getString("customerName"),
                    rs.getString("phoneNumber"),
                    rs.getString("password")
                );
                customers.add(c);
            }

            rs = stmt.executeQuery("SELECT * FROM vehicles");
            while (rs.next()) {
                vehicle v = CreateVehicle.create(
                    rs.getString("vehicleType"),
                    rs.getString("vehicleId"),
                    rs.getString("brand"),
                    rs.getString("model"),
                    rs.getDouble("basePricePerDay")
                );
                v.setAvailable(rs.getBoolean("isAvailable"));
                vehicles.add(v);
            }
        } catch (SQLException e) {
            System.out.println("DB Error (Load): " + e.getMessage());
        }
    }

    public void menuSystem() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- Vehicle Rental Menu ---");
            System.out.println("1. Create New Customer");
            System.out.println("2. Login and Rent Vehicle");
            System.out.println("3. Return a Vehicle");
            System.out.println("4. Exit");
            int choice = sc.nextInt();
            sc.nextLine();

            if (choice == 1) {
                System.out.print("Enter Customer ID: ");
                String id = sc.nextLine();
                System.out.print("Enter Name: ");
                String name = sc.nextLine();
                System.out.print("Enter Phone: ");
                String phone = sc.nextLine();
                System.out.print("Enter Password: ");
                String pass = sc.nextLine();
                customer c = new customer(id, name, phone, pass);
                addCustomer(c);
                System.out.println("Customer created successfully.");
            } else if (choice == 2) {
                System.out.print("Enter Customer ID: ");
                String id = sc.nextLine();
                System.out.print("Enter Password: ");
                String pass = sc.nextLine();
                customer found = null;
                for (customer c : customers) {
                    if (c.getCustomerId().equals(id) && c.validatePassword(pass)) {
                        found = c;
                        break;
                    }
                }
                if (found == null) {
                    System.out.println("Invalid credentials.");
                    continue;
                }
                System.out.print("Enter Vehicle Type (Car/Bike): ");
                String type = sc.nextLine();
                System.out.print("Enter Vehicle ID: ");
                String vId = sc.nextLine();
                System.out.print("Enter Brand: ");
                String brand = sc.nextLine();
                System.out.print("Enter Model: ");
                String model = sc.nextLine();
                System.out.print("Enter Price Per Day: ");
                double price = sc.nextDouble();
                System.out.print("Enter Number of Rental Days: ");
                int days = sc.nextInt();
                vehicle v = CreateVehicle.create(type, vId, brand, model, price);
                addVehicle(v);
                rentVehicle(v, found, days);
                System.out.println(type + " rented successfully for " + days + " days.");
            } else if (choice == 3) {
                System.out.print("Enter Vehicle ID to return: ");
                String vId = sc.nextLine();
                vehicle v = null;
                for (vehicle ve : vehicles) {
                    if (ve.getVehicleId().equals(vId) && !ve.isAvailable()) {
                        v = ve;
                        break;
                    }
                }
                if (v == null) {
                    System.out.println("No rented vehicle found with ID: " + vId);
                } else {
                    int rentedDays = 0;
                    for (Rental r : rentals) {
                        if (r.getVehicle() == v) {
                            rentedDays = r.getDays();
                            break;
                        }
                    }
                    double totalPrice = v.calculatePrice(rentedDays);
                    System.out.println("Total price for " + rentedDays + " days: ₹" + totalPrice);
                    returnVehicle(v);
                    System.out.println("Vehicle returned successfully.");
                }
            } else if (choice == 4) {
                System.out.println("Exiting...");
                break;
            } else {
                System.out.println("Invalid choice");
            }
        }
        sc.close();
    }
}

public class source {
    public static void main(String[] args) {
        VehicleRentalSystem system = new VehicleRentalSystem();
        system.loadData();
        system.menuSystem();
    }
}