package com.example.Banking.CLI;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.util.Base64;

@Service
public class BankingService {
    private final String USER_FILE = "users.txt";
    private final String TXN_FILE_PREFIX = "transactions_";

    public boolean registerUser(String username, String password) {
        try {
            if (Files.exists(Paths.get(USER_FILE))) {
                List<String> users = Files.readAllLines(Paths.get(USER_FILE));
                for (String user : users) {
                    String[] parts = user.split(",");
                    if (parts[0].equalsIgnoreCase(username)) return false;
                }
            }

            String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());
            Files.write(Paths.get(USER_FILE),
                    (username + "," + encodedPassword + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean loginUser(String username, String password) {
        try {
            String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());
            List<String> users = Files.readAllLines(Paths.get(USER_FILE));
            for (String user : users) {
                String[] parts = user.split(",");
                if (parts[0].equals(username) && parts[1].equals(encodedPassword)) {
                    return true;
                }
            }
        } catch (IOException ignored) {}
        return false;
    }

    public void recordTransaction(String username, String type, double amount) {
        List<Transaction> transactions = getTransactions(username);
        double balance = transactions.isEmpty() ? 0.0 : transactions.get(transactions.size() - 1).getBalance();
        balance += type.equals("CREDIT") ? amount : -amount;

        String timestamp = ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'"));

        String line = String.format("%s | %s | ₹%.2f | ₹%.2f", timestamp, type, amount, balance);
        try {
            Files.write(Paths.get(TXN_FILE_PREFIX + username + ".txt"),
                    (line + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    public List<Transaction> getTransactions(String username) {
        List<Transaction> list = new ArrayList<>();
        File file = new File(TXN_FILE_PREFIX + username + ".txt");
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                String time = parts[0].trim();
                String type = parts[1].trim();
                double amount = Double.parseDouble(parts[2].trim().replace("₹", ""));
                double balance = Double.parseDouble(parts[3].trim().replace("₹", ""));
                list.add(new Transaction(time, type, amount, balance));
            }
        } catch (IOException ignored) {}
        return list;
    }

    public List<Transaction> getFilteredTransactions(String username, String type) {
        List<Transaction> all = getTransactions(username);
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : all) {
            if (t.getType().equalsIgnoreCase(type)) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    public double getBalance(String username) {
        List<Transaction> transactions = getTransactions(username);
        if (transactions.isEmpty()) return 0.0;
        return transactions.get(transactions.size() - 1).getBalance();
    }
}
