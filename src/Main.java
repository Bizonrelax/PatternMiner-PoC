

public class Main {
    
    public static void main(String[] args) {
        System.out.println("=== PatternMiner v0.1 ===");
        System.out.println("Демонстрационная версия\n");
        
        if (args.length > 0) {
            switch (args[0]) {
                case "--gui":
                    launchGUI();
                    break;
                case "--test-crypto":
                    testCrypto();
                    break;
                case "--compress":
                    if (args.length > 1) {
                        testCompression(args[1]);
                    } else {
                        System.out.println("Укажите файл: --compress файл.txt");
                    }
                    break;
                case "--help":
                default:
                    printHelp();
            }
        } else {
            showMenu();
        }
    }
    
    private static void showMenu() {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        
        while (true) {
            System.out.println("\n=== PatternMiner Меню ===");
            System.out.println("1. Запустить GUI (майнинг паттернов)");
            System.out.println("2. Тест криптовалюты (PatternCoin)");
            System.out.println("3. Тест сжатия файла");
            System.out.println("4. Тест долговременного хранения");
            System.out.println("5. Комбинированный майнинг");
            System.out.println("6. Выйти");
            System.out.print("Выберите: ");
            
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1":
                    launchGUI();
                    break;
                case "2":
                    testCrypto();
                    break;
                case "3":
                    System.out.print("Введите путь к файлу: ");
                    String file = scanner.nextLine();
                    testCompression(file);
                    break;
                case "4":
                    testStorage();
                    break;
                case "5":
                    testCombinedMining();
                    break;
                case "6":
                    System.out.println("Выход...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Неверный выбор");
            }
        }
    }
    
    private static void launchGUI() {
        System.out.println("Запуск графического интерфейса...");
        try {
            // Запускаем GUI
            Class<?> guiClass = Class.forName("gui.SimpleGUI");
            java.lang.reflect.Method mainMethod = guiClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[]{});
        } catch (Exception e) {
            System.out.println("Ошибка запуска GUI: " + e.getMessage());
        }
    }
    
    private static void testCrypto() {
        System.out.println("\n=== Тест PatternCoin ===");
        try {
            Class<?> coinClass = Class.forName("crypto.PatternCoin");
            java.lang.reflect.Method testMethod = coinClass.getMethod("test");
            testMethod.invoke(null);
        } catch (Exception e) {
            System.out.println("Ошибка запуска теста криптовалюты: " + e.getMessage());
        }
    }
    
    private static void testStorage() {
        System.out.println("\n=== Тест долговременного хранения ===");
        try {
            Class<?> storageClass = Class.forName("crypto.StorageReward");
            java.lang.reflect.Method method = storageClass.getMethod("demo");
            method.invoke(null);
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
    
    private static void testCombinedMining() {
        System.out.println("\n=== Тест комбинированного майнинга ===");
        System.out.println("(В разработке)");
    }
    
    private static void testCompression(String filename) {
        System.out.println("\n=== Тест сжатия ===");
        System.out.println("Функция сжатия в разработке...");
        System.out.println("Проверка файла: " + filename);
        
        // Простая проверка файла
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filename);
            if (java.nio.file.Files.exists(path)) {
                long size = java.nio.file.Files.size(path);
                System.out.println("Размер файла: " + size + " байт");
                
                // Простое BASE64 кодирование для демонстрации
                byte[] data = java.nio.file.Files.readAllBytes(path);
                String base64 = java.util.Base64.getEncoder().encodeToString(data);
                System.out.println("BASE64 размер: " + base64.length() + " символов");
                
                // Простое сжатие (имитация)
                String compressed = base64.replaceAll("(.)\\1{2,}", "$1{$1count}");
                System.out.println("После простого сжатия: " + compressed.length() + " символов");
                
                if (compressed.length() < base64.length()) {
                    double improvement = (1.0 - (double)compressed.length() / base64.length()) * 100;
                    System.out.printf("✅ Улучшение: %.1f%%%n", improvement);
                }
            } else {
                System.out.println("Файл не найден");
            }
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
    
    private static void printHelp() {
        System.out.println("Использование:");
        System.out.println("  java Main                    - Интерактивное меню");
        System.out.println("  java Main --gui              - Графический интерфейс");
        System.out.println("  java Main --test-crypto      - Тест криптовалюты");
        System.out.println("  java Main --compress файл    - Тест сжатия файла");
        System.out.println("  java Main --help             - Эта справка");
    }
}