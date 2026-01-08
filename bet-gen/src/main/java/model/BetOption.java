package model;

public class BetOption {

    private String name;
    private double price;
    private Double point; // for goals in match

    public BetOption() {}

    public BetOption(String name, double price, Double point) {
        this.name = name;
        this.price = price;
        this.point = point;
    }


    public String getName() { return name; }
    public double getPrice() { return price; }
    public Double getPoint() { return point; }

}
