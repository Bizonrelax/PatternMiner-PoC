import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class MultiLevelCompressor {
    
    // Многоуровневое сжатие с адаптивным выбором преобразований
    public static MultiLevelResult compressMultiLevel(byte[] data, int maxLevels) {
        System.out.println("=== МНОГОУРОВНЕВОЕ СЖАТИЕ ===");
        System.out.println("Максимум уровней: " + maxLevels);
        System.out.println();
        
        List<CompressionLevel> levels = new ArrayList<>();
        byte[] currentData = data;
        int level = 0;
        
        while (level < maxLevels) {
            level++;
            System.out.println("Уровень " + level + ":");
            System.out.println("-".repeat(40));
            
            // Анализируем текущие данные
            String currentString = new String(currentData, StandardCharsets.UTF_8);
            DigitalGeologyCompressor.PatternAnalysis analysis = 
                DigitalGeologyCompressor.analyzePatterns(currentString);
            
            // Выбираем преобразование
            String transform = selectTransformForLevel(analysis, level);
            
            // Применяем преобразование и сжимаем
            String transformed = applyTransform(currentString, transform);
            byte[] compressed = compressZip(transformed);
            
            // Проверяем, есть ли выигрыш
            double ratio = (double) compressed.length / currentData.length;
            
            CompressionLevel levelResult = new CompressionLevel(
                level, transform, currentData.length, 
                compressed.length, ratio, analysis.dataType
            );
            
            levels.add(levelResult);
            levelResult.printStats();
            
            // Если сжатие ухудшилось, останавливаемся
            if (ratio >= 0.95 && level > 1) {
                System.out.println("⚠️ Сжатие ухудшилось, останавливаемся.");
                break;
            }
            
            // Для следующего уровня
            currentData = compressed;
            
            // Если достигли минимального размера
            if (compressed.length < 100) {
                System.out.println("✅ Достигнут минимальный размер.");
                break;
            }
        }
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ИТОГ МНОГОУРОВНЕВОГО СЖАТИЯ:");
        
        int totalReduction = data.length - currentData.length;
        double totalRatio = (double) currentData.length / data.length;
        
        System.out.printf("Исходный размер: %d байт%n", data.length);
        System.out.printf("Финальный размер: %d байт%n", currentData.length);
        System.out.printf("Общее сжатие: %d байт (%.2f%%)%n", 
            totalReduction, (1 - totalRatio) * 100);
        System.out.printf("Коэффициент сжатия: %.4f%n", totalRatio);
        System.out.println("Уровней использовано: " + levels.size());
        
        return new MultiLevelResult(currentData, levels, totalRatio);
    }
    
    // Выбор преобразования для уровня
    private static String selectTransformForLevel(
            DigitalGeologyCompressor.PatternAnalysis analysis, int level) {
        
        // На разных уровнях используем разные стратегии
        switch (level % 4) {
            case 0:
                return "GROUP_BY_FREQUENCY";
            case 1:
                return "BWT";
            case 2:
                if (analysis.entropy < 3.0) return "RLE";
                return "SORT_ASC";
            case 3:
                return "PATTERN_COMPRESSION";
            default:
                return "GROUP_BY_FREQUENCY";
        }
    }
    
    private static String applyTransform(String data, String transform) {
        switch (transform) {
            case "GROUP_BY_FREQUENCY":
                return groupByFrequency(data);
            case "BWT":
                try {
                    BWTTransformer.BWTResult bwt = BWTTransformer.forwardBWT(data);
                    return bwt.transformed;
                } catch (Exception e) {
                    return data;
                }
            case "SORT_ASC":
                char[] sorted = data.toCharArray();
                Arrays.sort(sorted);
                return new String(sorted);
            case "RLE":
                return runLengthEncode(data);
            case "PATTERN_COMPRESSION":
                return compressPatterns(data);
            default:
                return data;
        }
    }
    
    // Компрессия паттернов
    private static String compressPatterns(String data) {
        StringBuilder result = new StringBuilder(data);
        
        // Ищем часто повторяющиеся подстроки длиной 3-6 символов
        for (int len = 6; len >= 3; len--) {
            Map<String, Integer> patternCounts = new HashMap<>();
            
            for (int i = 0; i <= data.length() - len; i++) {
                String pattern = data.substring(i, i + len);
                patternCounts.put(pattern, patternCounts.getOrDefault(pattern, 0) + 1);
            }
            
            // Заменяем часто встречающиеся паттерны
            for (Map.Entry<String, Integer> entry : patternCounts.entrySet()) {
                if (entry.getValue() >= 3) {
                    String pattern = entry.getKey();
                    // Заменяем на короткий код
                    String replacement = "«" + (char)('A' + len) + "»";
                    String temp = result.toString();
                    result = new StringBuilder(temp.replace(pattern, replacement));
                }
            }
        }
        
        return result.toString();
    }
    
    private static String groupByFrequency(String data) {
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : data.toCharArray()) freq.put(c, freq.getOrDefault(c, 0) + 1);
        
        List<Character> chars = new ArrayList<>(freq.keySet());
        chars.sort((a, b) -> Integer.compare(freq.get(b), freq.get(a)));
        
        StringBuilder result = new StringBuilder();
        for (char c : chars) {
            for (int i = 0; i < freq.get(c); i++) result.append(c);
        }
        return result.toString();
    }
    
    private static String runLengthEncode(String data) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < data.length()) {
            char current = data.charAt(i);
            int count = 1;
            
            while (i + count < data.length() && data.charAt(i + count) == current) {
                count++;
            }
            
            if (count > 3) {
                result.append(current).append('[').append(count).append(']');
                i += count;
            } else {
                for (int j = 0; j < count; j++) result.append(current);
                i += count;
            }
        }
        
        return result.toString();
    }
    
    private static byte[] compressZip(String data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(data.getBytes(StandardCharsets.UTF_8));
            }
            return baos.toByteArray();
        } catch (Exception e) {
            return data.getBytes(StandardCharsets.UTF_8);
        }
    }
    
    // Классы для хранения результатов
    public static class MultiLevelResult {
        public final byte[] finalData;
        public final List<CompressionLevel> levels;
        public final double totalRatio;
        
        public MultiLevelResult(byte[] finalData, List<CompressionLevel> levels, 
                               double totalRatio) {
            this.finalData = finalData;
            this.levels = levels;
            this.totalRatio = totalRatio;
        }
        
        public void saveToFile(String filename) throws IOException {
            Files.write(Paths.get(filename), finalData);
            System.out.println("✅ Данные сохранены в файл: " + filename);
        }
    }
    
    public static class CompressionLevel {
        public final int level;
        public final String transform;
        public final int inputSize;
        public final int outputSize;
        public final double ratio;
        public final DigitalGeologyCompressor.DataType dataType;
        
        public CompressionLevel(int level, String transform, int inputSize,
                               int outputSize, double ratio,
                               DigitalGeologyCompressor.DataType dataType) {
            this.level = level;
            this.transform = transform;
            this.inputSize = inputSize;
            this.outputSize = outputSize;
            this.ratio = ratio;
            this.dataType = dataType;
        }
        
        public void printStats() {
            System.out.printf("Преобразование: %s%n", transform);
            System.out.printf("Вход: %d байт, Выход: %d байт%n", inputSize, outputSize);
            System.out.printf("Коэффициент: %.4f (сжато на %.1f%%)%n", 
                ratio, (1 - ratio) * 100);
            System.out.printf("Тип данных: %s%n", dataType);
            System.out.println();
        }
    }
}