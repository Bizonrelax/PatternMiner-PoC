package crypto;

import java.security.*;
import java.util.*;

public class PatternCoin {
    
    // Упрощённая версия для теста
    public static class SimpleWallet {
        public String address;
        public double balance;
        
        public SimpleWallet() {
            this.address = "user_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
            this.balance = 0;
        }
        
        public void addBalance(double amount) {
            this.balance += amount;
            System.out.println("Кошелёк " + address + ": +" + amount + " PTC, баланс: " + balance);
        }
    }
    
    public static class MiningSystem {
        private List<SimpleWallet> miners;
        private int totalPatternsFound;
        
        public MiningSystem() {
            this.miners = new ArrayList<>();
            this.totalPatternsFound = 0;
        }
        
        public void addMiner(SimpleWallet miner) {
            miners.add(miner);
            System.out.println("Добавлен майнер: " + miner.address);
        }
        
        public void minePattern(String pattern, SimpleWallet miner) {
            totalPatternsFound++;
            
            // Награда зависит от сложности паттерна
            double reward = pattern.length() * 0.1;
            reward = Math.max(1.0, Math.min(reward, 10.0));
            
            miner.addBalance(reward);
            
            System.out.println("Найден паттерн: " + pattern);
            System.out.println("Награда: " + reward + " PTC");
            System.out.println("Всего паттернов найдено: " + totalPatternsFound);
        }
        
        public void printStats() {
            System.out.println("\n=== Статистика майнинга ===");
            System.out.println("Всего майнеров: " + miners.size());
            System.out.println("Всего паттернов: " + totalPatternsFound);
            
            for (SimpleWallet wallet : miners) {
                System.out.printf("Майнер %s: %.2f PTC%n", 
                    wallet.address.substring(0, Math.min(15, wallet.address.length())), 
                    wallet.balance);
            }
        }
    }
    
    // Тест системы
    public static void test() {
        System.out.println("=== PatternCoin Demo ===");
        
        MiningSystem system = new MiningSystem();
        
        SimpleWallet miner1 = new SimpleWallet();
        SimpleWallet miner2 = new SimpleWallet();
        
        system.addMiner(miner1);
        system.addMiner(miner2);
        
        String[] patterns = {
            "AAA{10}BBB{10}",
            "XYZ{5}123{5}",
            "ABCD{8}",
            "[0-9]{4}[A-Z]{3}",
            "hello{3}world{3}"
        };
        
        System.out.println("\n=== Начало майнинга ===");
        
        for (int i = 0; i < 5; i++) {
            System.out.println("\nЦикл майнинга " + (i + 1) + ":");
            
            SimpleWallet miner = (Math.random() > 0.5) ? miner1 : miner2;
            String pattern = patterns[i % patterns.length];
            
            system.minePattern(pattern, miner);
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        system.printStats();
    }
    
    // Методы для долговременного хранения
    public static void storageDemo() {
        System.out.println("\n=== ДЕМО СИСТЕМЫ ХРАНЕНИЯ ===");
        System.out.println("(Реализация в следующей версии)");
    }
    
    public static void combinedMiningDemo() {
        System.out.println("\n=== КОМБИНИРОВАННЫЙ МАЙНИНГ ===");
        System.out.println("(Реализация в следующей версии)");
    }
}