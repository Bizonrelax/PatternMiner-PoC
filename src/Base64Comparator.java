import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class Base64Comparator {
    
    // Метод для честного сравнения BASE64 -> Преобразование -> ZIP -> BASE64
    public static ComparisonResult compareBase64ToBase64(String originalBase64, 
                                                         String transformName,
                                                         String transformedData) {
        
        // 1. Исходный размер BASE64
        int originalSize = originalBase64.length();
        
        // 2. Сжимаем исходный BASE64 ZIP'ом
        byte[] originalCompressed = compressZip(originalBase64);
        String originalCompressedBase64 = Base64.getEncoder().encodeToString(originalCompressed);
        
        // 3. Сжимаем преобразованные данные ZIP'ом
        byte[] transformedCompressed = compressZip(transformedData);
        String transformedCompressedBase64 = Base64.getEncoder().encodeToString(transformedCompressed);
        
        // 4. Сравниваем размеры BASE64
        int originalCompressedSize = originalCompressedBase64.length();
        int transformedCompressedSize = transformedCompressedBase64.length();
        
        // 5. Вычисляем эффективность
        double originalRatio = (double) originalCompressedSize / originalSize;
        double transformedRatio = (double) transformedCompressedSize / originalSize;
        double improvement = (1.0 - (double) transformedCompressedSize / originalCompressedSize) * 100;
        
        return new ComparisonResult(
            transformName,
            originalSize,
            originalCompressedSize,
            transformedCompressedSize,
            originalRatio,
            transformedRatio,
            improvement
        );
    }
    
    // Тест всех преобразований на честность
    public static void testAllTransformsHonestly(String originalBase64) {
        FileLogger.log("\n=== ЧЕСТНОЕ СРАВНЕНИЕ BASE64-TO-BASE64 ===");
        FileLogger.log("Исходные данные: " + originalBase64.length() + " символов BASE64");
        FileLogger.log("=".repeat(100));
        
        List<ComparisonResult> results = new ArrayList<>();
        
        // Тестируем разные преобразования
        results.add(testTransform(originalBase64, "Без преобразования", originalBase64));
        
        // Группировка символов
        String grouped = groupSimilar(originalBase64);
        results.add(testTransform(originalBase64, "Группировка", grouped));
        
        // Сортировка по возрастанию
        char[] sorted = originalBase64.toCharArray();
        Arrays.sort(sorted);
        results.add(testTransform(originalBase64, "Сортировка возр", new String(sorted)));
        
        // RLE
        String rle = simpleRLE(originalBase64);
        results.add(testTransform(originalBase64, "RLE", rle));
        
        // BWT
        try {
            BWTTransformer.BWTResult bwt = BWTTransformer.forwardBWT(originalBase64);
            results.add(testTransform(originalBase64, "BWT", bwt.transformed));
        } catch (Exception e) {
            FileLogger.log("BWT не удалось: " + e.getMessage());
        }
        
        // Сортируем результаты по эффективности
        results.sort((a, b) -> Double.compare(a.transformedRatio, b.transformedRatio));
        
        // Выводим таблицу
        FileLogger.log(String.format("%-25s | %10s | %10s | %10s | %10s | %10s",
            "Преобразование", "Исходный", "ZIP+B64", "Наш+B64", "Коэфф.", "Выигрыш"));
        FileLogger.log("-".repeat(100));
        
        for (ComparisonResult r : results) {
            String improvementStr = r.improvement > 0 ? 
                String.format("+%.2f%%", r.improvement) : 
                String.format("%.2f%%", r.improvement);
                
            FileLogger.log(String.format("%-25s | %10d | %10d | %10d | %10.4f | %10s",
                r.transformName,
                r.originalSize,
                r.originalCompressedSize,
                r.transformedCompressedSize,
                r.transformedRatio,
                improvementStr));
        }
        
        // Показываем лучший результат
        ComparisonResult best = results.get(0);
        FileLogger.log("\n" + "=".repeat(100));
        FileLogger.log("ЛУЧШИЙ РЕЗУЛЬТАТ: " + best.transformName);
        FileLogger.log(String.format("Выигрыш против чистого ZIP: %.2f%%", best.improvement));
        FileLogger.log(String.format("Коэффициент сжатия: %.4f", best.transformedRatio));
        
        if (best.improvement > 0) {
            FileLogger.log("✅ НАШ МЕТОД ЭФФЕКТИВЕН для BASE64-to-BASE64!");
        } else {
            FileLogger.log("❌ ZIP всё ещё лучше без преобразований.");
        }
    }
    
    private static ComparisonResult testTransform(String original, String name, String transformed) {
        return compareBase64ToBase64(original, name, transformed);
    }
    
    private static String groupSimilar(String input) {
        // Подсчитываем частоты символов
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : input.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        
        // Собираем символы в порядке убывания частоты
        List<Character> chars = new ArrayList<>(freq.keySet());
        chars.sort((a, b) -> Integer.compare(freq.get(b), freq.get(a)));
        
        StringBuilder result = new StringBuilder();
        // Сначала записываем символы в порядке частоты
        for (char c : chars) {
            for (int i = 0; i < freq.get(c); i++) {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    private static String simpleRLE(String input) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < input.length()) {
            char current = input.charAt(i);
            int count = 1;
            
            while (i + count < input.length() && input.charAt(i + count) == current) {
                count++;
            }
            
            if (count > 3) {
                result.append("[").append(current).append(count).append("]");
                i += count;
            } else {
                for (int j = 0; j < count; j++) {
                    result.append(current);
                }
                i += count;
            }
        }
        
        return result.toString();
    }
    
    private static byte[] compressZip(String input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(input.getBytes(StandardCharsets.UTF_8));
            }
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
    
    // Класс для хранения результатов сравнения
    public static class ComparisonResult {
        public final String transformName;
        public final int originalSize;
        public final int originalCompressedSize;
        public final int transformedCompressedSize;
        public final double originalRatio;
        public final double transformedRatio;
        public final double improvement;
        
        public ComparisonResult(String transformName, int originalSize,
                               int originalCompressedSize, int transformedCompressedSize,
                               double originalRatio, double transformedRatio,
                               double improvement) {
            this.transformName = transformName;
            this.originalSize = originalSize;
            this.originalCompressedSize = originalCompressedSize;
            this.transformedCompressedSize = transformedCompressedSize;
            this.originalRatio = originalRatio;
            this.transformedRatio = transformedRatio;
            this.improvement = improvement;
        }
    }
}