import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class DigitalGeologyCompressor {
    
    // Основной метод сжатия с обнаружением паттернов
    public static CompressionResult compressWithPatternDetection(byte[] data) {
        System.out.println("=== ЦИФРОВАЯ ГЕОЛОГИЯ: АНАЛИЗ ПАТТЕРНОВ ===");
        System.out.println("Размер входных данных: " + data.length + " байт");
        
        // Конвертируем в строку для анализа (если это текст/BASE64)
        String dataString = new String(data, StandardCharsets.UTF_8);
        
        // Анализируем паттерны
        PatternAnalysis analysis = analyzePatterns(dataString);
        System.out.println("\nОбнаруженные паттерны:");
        analysis.printAnalysis();
        
        // Выбираем оптимальное преобразование
        String bestTransform = selectBestTransform(analysis);
        System.out.println("\nВыбрано преобразование: " + bestTransform);
        
        // Применяем преобразование
        String transformed = applyTransform(dataString, bestTransform);
        
        // Сжимаем
        byte[] compressed = compressZip(transformed);
        
        // Кодируем в BASE64 для сравнения
        String base64Original = Base64.getEncoder().encodeToString(data);
        String base64Compressed = Base64.getEncoder().encodeToString(compressed);
        
        double ratio = (double) compressed.length / data.length;
        double base64Ratio = (double) base64Compressed.length() / base64Original.length();
        
        System.out.println("\nРезультаты сжатия:");
        System.out.printf("Исходный размер: %d байт (BASE64: %d символов)%n", 
            data.length, base64Original.length());
        System.out.printf("Сжатый размер: %d байт (BASE64: %d символов)%n", 
            compressed.length, base64Compressed.length());
        System.out.printf("Коэффициент сжатия: %.4f%n", ratio);
        System.out.printf("Выигрыш в BASE64: %.2f%%%n", (1 - base64Ratio) * 100);
        
        return new CompressionResult(compressed, bestTransform, analysis);
    }
    
    // Анализ паттернов в данных
    public static PatternAnalysis analyzePatterns(String data) {
        PatternAnalysis analysis = new PatternAnalysis();
        
        // 1. Анализ частот символов
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : data.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        analysis.charFrequency = freq;
        
        // 2. Самые частые символы
        List<Map.Entry<Character, Integer>> sortedFreq = new ArrayList<>(freq.entrySet());
        sortedFreq.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        analysis.mostCommonChars = sortedFreq;
        
        // 3. Поиск повторяющихся последовательностей
        analysis.repetitionPatterns = findRepetitions(data);
        
        // 4. Энтропия
        analysis.entropy = calculateEntropy(freq, data.length());
        
        // 5. Определяем тип данных
        analysis.dataType = determineDataType(data, freq);
        
        return analysis;
    }
    
    // Поиск повторяющихся последовательностей
    private static List<RepetitionPattern> findRepetitions(String data) {
        List<RepetitionPattern> patterns = new ArrayList<>();
        
        for (int patternLength = 2; patternLength <= 10; patternLength++) {
            Map<String, Integer> patternCounts = new HashMap<>();
            
            for (int i = 0; i <= data.length() - patternLength; i++) {
                String pattern = data.substring(i, i + patternLength);
                patternCounts.put(pattern, patternCounts.getOrDefault(pattern, 0) + 1);
            }
            
            // Ищем паттерны, которые повторяются минимум 3 раза
            for (Map.Entry<String, Integer> entry : patternCounts.entrySet()) {
                if (entry.getValue() >= 3) {
                    patterns.add(new RepetitionPattern(entry.getKey(), entry.getValue()));
                }
            }
        }
        
        // Сортируем по частоте и длине
        patterns.sort((a, b) -> {
            int freqCompare = Integer.compare(b.count, a.count);
            if (freqCompare != 0) return freqCompare;
            return Integer.compare(b.pattern.length(), a.pattern.length());
        });
        
        return patterns;
    }
    
    // Выбор оптимального преобразования
    private static String selectBestTransform(PatternAnalysis analysis) {
        // Правила выбора на основе анализа
        if (analysis.dataType == DataType.BASE64) {
            // Для BASE64 данных
            if (analysis.entropy > 4.5) {
                return "GROUP_BY_FREQUENCY"; // Для случайных BASE64
            } else if (analysis.repetitionPatterns.size() > 0) {
                return "PATTERN_COMPRESSION"; // Есть повторяющиеся паттерны
            } else {
                return "BWT"; // Для структурированных данных
            }
        } else if (analysis.dataType == DataType.TEXT) {
            // Для текста
            if (analysis.mostCommonChars.get(0).getValue() > analysis.totalChars * 0.2) {
                return "RLE"; // Много повторений одного символа
            } else {
                return "HUFFMAN_LIKE"; // Текстовые данные
            }
        } else {
            // Для бинарных данных
            return "DELTA_ENCODING";
        }
    }
    
    // Применение преобразования
    private static String applyTransform(String data, String transform) {
        switch (transform) {
            case "GROUP_BY_FREQUENCY":
                return groupByFrequency(data);
            case "SORT_ASC":
                char[] sorted = data.toCharArray();
                Arrays.sort(sorted);
                return new String(sorted);
            case "BWT":
                try {
                    BWTTransformer.BWTResult bwt = BWTTransformer.forwardBWT(data);
                    return bwt.transformed;
                } catch (Exception e) {
                    return data;
                }
            case "RLE":
                return runLengthEncode(data);
            default:
                return data;
        }
    }
    
    // Группировка по частоте символов
    private static String groupByFrequency(String data) {
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : data.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        
        List<Character> chars = new ArrayList<>(freq.keySet());
        chars.sort((a, b) -> Integer.compare(freq.get(b), freq.get(a)));
        
        StringBuilder result = new StringBuilder();
        for (char c : chars) {
            for (int i = 0; i < freq.get(c); i++) {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    // RLE кодирование
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
                result.append(current).append('{').append(count).append('}');
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
    
    // Расчёт энтропии
    private static double calculateEntropy(Map<Character, Integer> freq, int total) {
        double entropy = 0.0;
        for (int count : freq.values()) {
            double p = (double) count / total;
            entropy -= p * Math.log(p) / Math.log(2);
        }
        return entropy;
    }
    
    // Определение типа данных
    private static DataType determineDataType(String data, Map<Character, Integer> freq) {
        // Проверяем BASE64
        String base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
        boolean isBase64 = true;
        for (char c : data.toCharArray()) {
            if (base64Chars.indexOf(c) == -1) {
                isBase64 = false;
                break;
            }
        }
        
        if (isBase64) return DataType.BASE64;
        
        // Проверяем текст (печатные символы)
        int printable = 0;
        for (char c : data.toCharArray()) {
            if (c >= 32 && c <= 126) printable++;
        }
        
        if ((double) printable / data.length() > 0.9) {
            return DataType.TEXT;
        }
        
        return DataType.BINARY;
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
    
    // Класс для хранения результатов
    public static class CompressionResult {
        public final byte[] compressedData;
        public final String usedTransform;
        public final PatternAnalysis analysis;
        
        public CompressionResult(byte[] compressedData, String usedTransform, 
                                PatternAnalysis analysis) {
            this.compressedData = compressedData;
            this.usedTransform = usedTransform;
            this.analysis = analysis;
        }
    }
    
    // Класс для анализа паттернов
    public static class PatternAnalysis {
        Map<Character, Integer> charFrequency;
        List<Map.Entry<Character, Integer>> mostCommonChars;
        List<RepetitionPattern> repetitionPatterns;
        double entropy;
        DataType dataType;
        int totalChars;
        public PatternAnalysis() {
            this.totalChars = 0;
        }
        void printAnalysis() {
            System.out.println("Тип данных: " + dataType);
            System.out.printf("Энтропия: %.3f бит/символ%n", entropy);
            System.out.println("Топ-5 символов:");
            for (int i = 0; i < Math.min(5, mostCommonChars.size()); i++) {
                Map.Entry<Character, Integer> entry = mostCommonChars.get(i);
                System.out.printf("  '%c' (код %3d): %d раз (%.1f%%)%n",
                    entry.getKey(), (int) entry.getKey(), entry.getValue(),
                    entry.getValue() * 100.0 / totalChars);
            }
            
            if (!repetitionPatterns.isEmpty()) {
                System.out.println("Обнаружены повторяющиеся паттерны:");
                for (int i = 0; i < Math.min(3, repetitionPatterns.size()); i++) {
                    RepetitionPattern p = repetitionPatterns.get(i);
                    System.out.printf("  \"%s\" - повторяется %d раз%n", 
                        p.pattern, p.count);
                }
            }
        }
    }
    
    // Вспомогательные классы
    public static class RepetitionPattern {
        String pattern;
        int count;
        
        RepetitionPattern(String pattern, int count) {
            this.pattern = pattern;
            this.count = count;
        }
    }
    
    public enum DataType {
        BASE64, TEXT, BINARY
    }
}