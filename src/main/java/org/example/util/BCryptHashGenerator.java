package org.example.util;

public class BCryptHashGenerator {
    public static void main(String[] args) {
        String[] passwords = {"Admin@123", "Employee@123", "Teamlead@123", "Manager@123"};
        for (String pwd : passwords) {
            System.out.println(pwd);
        }
    }
}
