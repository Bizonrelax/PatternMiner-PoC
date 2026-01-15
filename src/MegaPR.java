import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MegaPR {
    // Гигантский ПР: мапа "сигнатура паттерна" -> "метод обработки"
    private static final Map<String, TransformMethod> PATTERN_DB = new HashMap<>();
    
    // Метод обработки паттерна
    interface TransformMethod {
        String encode(String input, Map<String, Object> params);
        String decode(String encoded, Map<String, Object> params);
        String getSignature();
    }
    
    static {
        // Инициализируем ПР тысячами паттернов (в реальности это терабайты)
        initPatternDatabase();
    }
    
    private static void initPatternDatabase() {
        // Паттерн 1: Длинные последовательности 'A' (частые в BASE64)
        PATTERN_DB.put("AAA_PATTERN", new TransformMethod() {
            @Override
            public String encode(String input, Map<String, Object> params) {
                // Заменяем последовательности из 3+ 'A' на специальный маркер
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (int i = 0; i < input.length(); i++) {
                    if (input.charAt(i) == 'A') {
                        count++;
                    } else {
                        if (count >= 3) {
                            sb.append("§A").append(count).append("§");
                            params.put("AAA_" + (i-count), count);
                        } else if (count > 0) {
                            for (int j = 0; j < count; j++) sb.append('A');
                        }
                        count = 0;
                        sb.append(input.charAt(i));
                    }
                }
                if (count >= 3) {
                    sb.append("§A").append(count).append("§");
                }
                return sb.toString();
            }
            
            @Override
            public String decode(String encoded, Map<String, Object> params) {
                // Восстанавливаем 'A'
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < encoded.length(); i++) {
                    if (encoded.charAt(i) == '§' && i+1 < encoded.length() && encoded.charAt(i+1) == 'A') {
                        int end = encoded.indexOf("§", i+2);
                        if (end > 0) {
                            int count = Integer.parseInt(encoded.substring(i+2, end));
                            for (int j = 0; j < count; j++) sb.append('A');
                            i = end;
                        } else {
                            sb.append(encoded.charAt(i));
                        }
                    } else {
                        sb.append(encoded.charAt(i));
                    }
                }
                return sb.toString();
            }
            
            @Override
            public String getSignature() { return "AAA_PATTERN"; }
        });
        
        // Паттерн 2: BWT с оптимизацией
        PATTERN_DB.put("BWT_OPTIMIZED", new TransformMethod() {
            @Override
            public String encode(String input, Map<String, Object> params) {
                BWTTransformer.BWTResult bwt = BWTTransformer.forwardBWT(input);
                params.put("bwt_index", bwt.index);
                return bwt.transformed;
            }
            
            @Override
            public String decode(String encoded, Map<String, Object> params) {
                int index = (int) params.get("bwt_index");
                return BWTTransformer.inverseBWT(encoded, index);
            }
            
            @Override
            public String getSignature() { return "BWT_OPTIMIZED"; }
        });
        
        // Паттерн 3: RUN-LENGTH для повторов
        PATTERN_DB.put("RLE_ADVANCED", new TransformMethod() {
            @Override
            public String encode(String input, Map<String, Object> params) {
                StringBuilder sb = new StringBuilder();
                int i = 0;
                while (i < input.length()) {
                    int j = i;
                    while (j < input.length() && input.charAt(j) == input.charAt(i)) {
                        j++;
                    }
                    int runLength = j - i;
                    if (runLength > 2) {
                        sb.append("«").append(input.charAt(i)).append(runLength).append("»");
                        i = j;
                    } else {
                        sb.append(input.charAt(i));
                        i++;
                    }
                }
                return sb.toString();
            }
            
            @Override
            public String decode(String encoded, Map<String, Object> params) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < encoded.length(); i++) {
                    if (encoded.charAt(i) == '«' && i+2 < encoded.length()) {
                        char ch = encoded.charAt(i+1);
                        int end = encoded.indexOf("»", i+2);
                        if (end > 0) {
                            int count = Integer.parseInt(encoded.substring(i+2, end));
                            for (int j = 0; j < count; j++) sb.append(ch);
                            i = end;
                        } else {
                            sb.append(encoded.charAt(i));
                        }
                    } else {
                        sb.append(encoded.charAt(i));
                    }
                }
                return sb.toString();
            }
            
            @Override
            public String getSignature() { return "RLE_ADVANCED"; }
        });
    }
    
    // Многоуровневое сжатие с использованием ПР
    public static class CompressionResult {
        public final byte[] data;
        public final List<String> usedPatterns;
        public final List<Map<String, Object>> patternParams;
        public final int originalSize;
        
        public CompressionResult(byte[] data, List<String> patterns, 
                                List<Map<String, Object>> params, int originalSize) {
            this.data = data;
            this.usedPatterns = patterns;
            this.patternParams = params;
            this.originalSize = originalSize;
        }
        
        public double getCompressionRatio() {
            return (double) data.length / originalSize;
        }
    }
    
    // Циклическое сжатие
    public static CompressionResult compressCyclic(String input, int maxCycles) {
        String current = input;
        List<String> usedPatterns = new ArrayList<>();
        List<Map<String, Object>> allParams = new ArrayList<>();
        
        System.out.println("=== ЦИКЛИЧЕСКОЕ СЖАТИЕ ===");
        System.out.printf("Начальный размер: %d символов%n", input.length());
        
        for (int cycle = 0; cycle < maxCycles; cycle++) {
            System.out.printf("%nЦикл %d:%n", cycle + 1);
            System.out.println("-".repeat(40));
            
            // 1. Анализируем данные, выбираем лучший паттерн из ПР
            String bestPattern = selectBestPattern(current);
            TransformMethod method = PATTERN_DB.get(bestPattern);
            
            if (method == null) break;
            
            // 2. Применяем преобразование
            Map<String, Object> params = new HashMap<>();
            String transformed = method.encode(current, params);
            
            // 3. Проверяем, стало ли лучше
            byte[] compressed = compressWithZip(transformed);
            
            System.out.printf("Паттерн: %s%n", bestPattern);
            System.out.printf("Размер после преобразования: %d символов%n", transformed.length());
            System.out.printf("Размер после ZIP: %d байт%n", compressed.length);
            
            // Если размер увеличился - останавливаемся
            if (compressed.length >= current.length() * 0.95 && cycle > 0) {
                System.out.println("Достигнут локальный минимум, останавливаемся.");
                break;
            }
            
            // 4. Сохраняем для следующего цикла
            usedPatterns.add(bestPattern);
            allParams.add(params);
            current = transformed;
            
            // Для демо: после каждого цикла показываем первые 30 символов
            System.out.printf("Данные после цикла: %s...%n", 
                current.substring(0, Math.min(30, current.length())));
        }
        
        // Финальное сжатие
        byte[] finalCompressed = compressWithZip(current);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.printf("Использовано паттернов: %d%n", usedPatterns.size());
        System.out.printf("Финальный размер: %d байт%n", finalCompressed.length);
        System.out.printf("Коэффициент сжатия: %.3f%n", 
            (double) finalCompressed.length / input.length());
        
        return new CompressionResult(finalCompressed, usedPatterns, allParams, input.length());
    }
    
    // Распаковка
    public static String decompressCyclic(CompressionResult result) {
        System.out.println("\n=== ЦИКЛИЧЕСКАЯ РАСПАКОВКА ===");
        
        // 1. Распаковываем ZIP
        String current = decompressFromZip(result.data);
        
        // 2. Применяем паттерны в обратном порядке
        for (int i = result.usedPatterns.size() - 1; i >= 0; i--) {
            String patternName = result.usedPatterns.get(i);
            TransformMethod method = PATTERN_DB.get(patternName);
            if (method != null) {
                System.out.printf("Применяем паттерн %s...%n", patternName);
                current = method.decode(current, result.patternParams.get(i));
            }
        }
        
        System.out.printf("Восстановлено символов: %d%n", current.length());
        return current;
    }
    
    private static String selectBestPattern(String data) {
        // В реальном ПР здесь сложный анализ: ML, статистика
        // Здесь упрощённо: смотрим на частые паттерны
        
        // Считаем количество 'A'
        long countA = data.chars().filter(c -> c == 'A').count();
        double ratioA = (double) countA / data.length();
        
        // Считаем повторяющиеся символы
        int maxRun = 0;
        char runChar = 0;
        int currentRun = 1;
        for (int i = 1; i < data.length(); i++) {
            if (data.charAt(i) == data.charAt(i-1)) {
                currentRun++;
                if (currentRun > maxRun) {
                    maxRun = currentRun;
                    runChar = data.charAt(i);
                }
            } else {
                currentRun = 1;
            }
        }
        
        // Выбираем лучший паттерн
        if (ratioA > 0.1) {
            return "AAA_PATTERN";
        } else if (maxRun >= 3) {
            return "RLE_ADVANCED";
        } else {
            return "BWT_OPTIMIZED";
        }
    }
    
    private static byte[] compressWithZip(String data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(data.getBytes(StandardCharsets.UTF_8));
            }
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
    
    private static String decompressFromZip(byte[] data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
            }
            return baos.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
    
    // Тест на бесконечном цикле (с ограничением по времени)
    public static void testInfiniteCompression(String input, int maxSeconds) {
        System.out.println("=== ТЕСТ БЕСКОНЕЧНОГО СЖАТИЯ ===");
        System.out.println("(ограничение: " + maxSeconds + " секунд)");
        
        String current = input;
        int cycle = 0;
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < maxSeconds * 1000) {
            cycle++;
            
            // Выбираем случайный паттерн (в реальном ПР - интеллектуально)
            List<String> patterns = new ArrayList<>(PATTERN_DB.keySet());
            String pattern = patterns.get((int)(Math.random() * patterns.size()));
            TransformMethod method = PATTERN_DB.get(pattern);
            
            Map<String, Object> params = new HashMap<>();
            String transformed = method.encode(current, params);
            byte[] compressed = compressWithZip(transformed);
            
            double ratio = (double) compressed.length / current.length();
            
            System.out.printf("Цикл %4d: %s -> размер: %d, ratio: %.3f%n",
                cycle, pattern, compressed.length, ratio);
            
            // Если сжатие стало хуже - пробуем другой подход
            if (ratio >= 1.0) {
                System.out.println("  Сжатие ухудшилось, пробуем другой паттерн...");
                continue;
            }
            
            current = transformed;
            
            // Эксперимент: иногда пробуем распаковать и снова сжать другим методом
            if (cycle % 5 == 0) {
                String decompressed = method.decode(current, params);
                // Пробуем другой паттерн на распакованных данных
                String otherPattern = patterns.get((int)(Math.random() * patterns.size()));
                if (!otherPattern.equals(pattern)) {
                    TransformMethod otherMethod = PATTERN_DB.get(otherPattern);
                    Map<String, Object> otherParams = new HashMap<>();
                    current = otherMethod.encode(decompressed, otherParams);
                }
            }
        }
        
        System.out.printf("%nВсего циклов: %d%n", cycle);
        System.out.printf("Общее время: %.1f сек%n", 
            (System.currentTimeMillis() - startTime) / 1000.0);
    }
}