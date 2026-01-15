import java.util.*;

public class SelfDescribingTransform {
    
    // Преобразование, которое ВСТРАИВАЕТ параметры в данные
    public static String embedTransform(String input, String patternId) {
        switch (patternId) {
            case "SORT_EMBEDDED":
                return sortEmbedded(input);
            case "FREQ_GROUP_EMBEDDED":
                return frequencyGroupEmbedded(input);
            case "RUN_LENGTH_EMBEDDED":
                return runLengthEmbedded(input);
            case "PATTERN_CYCLE_EMBEDDED":
                return patternCycleEmbedded(input);
            default:
                return input;
        }
    }
    
    // Обратное преобразование
    public static String extractTransform(String embedded) {
        if (embedded.startsWith("SORT|")) {
            return extractSorted(embedded);
        } else if (embedded.startsWith("FREQ|")) {
            return extractFrequency(embedded);
        } else if (embedded.startsWith("RLE|")) {
            return extractRunLength(embedded);
        } else if (embedded.startsWith("CYC|")) {
            return extractCycle(embedded);
        }
        return embedded;
    }
    
    // 1. Сортировка с встроенной перестановкой
    private static String sortEmbedded(String input) {
        // Создаем пары (символ, позиция)
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < input.length(); i++) {
            pairs.add(new Pair(input.charAt(i), i));
        }
        
        // Сортируем по символу
        pairs.sort((a, b) -> Character.compare(a.ch, b.ch));
        
        // Собираем результат: префикс + отсортированные символы + позиции
        StringBuilder sorted = new StringBuilder();
        StringBuilder positions = new StringBuilder();
        
        for (Pair p : pairs) {
            sorted.append(p.ch);
            positions.append((char) p.pos); // Сохраняем позицию как char
        }
        
        return "SORT|" + sorted.toString() + "|" + positions.toString();
    }
    
    private static String extractSorted(String embedded) {
        // Формат: SORT|отсортированные_символы|позиции
        String[] parts = embedded.split("\\|", 3);
        if (parts.length < 3) return embedded;
        
        String sorted = parts[1];
        String positions = parts[2];
        
        // Восстанавливаем исходный порядок
        char[] result = new char[sorted.length()];
        for (int i = 0; i < sorted.length(); i++) {
            int pos = positions.charAt(i);
            result[pos] = sorted.charAt(i);
        }
        
        return new String(result);
    }
    
    // 2. Группировка по частоте с встроенной частотной таблицей
    private static String frequencyGroupEmbedded(String input) {
        // Считаем частоты
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : input.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        
        // Сортируем символы по частоте (часто встречающиеся первыми)
        List<Character> chars = new ArrayList<>(freq.keySet());
        chars.sort((a, b) -> Integer.compare(freq.get(b), freq.get(a)));
        
        // Создаем строку: символы по частоте + исходные данные с заменой на индексы
        StringBuilder header = new StringBuilder();
        for (char c : chars) {
            header.append(c);
        }
        
        // Кодируем данные: заменяем символы на их позиции в header
        StringBuilder encoded = new StringBuilder();
        for (char c : input.toCharArray()) {
            int index = chars.indexOf(c);
            encoded.append((char) index); // Сохраняем индекс как char
        }
        
        return "FREQ|" + header.toString() + "|" + encoded.toString();
    }
    
    private static String extractFrequency(String embedded) {
        String[] parts = embedded.split("\\|", 3);
        if (parts.length < 3) return embedded;
        
        String header = parts[1];
        String encoded = parts[2];
        
        // Восстанавливаем
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < encoded.length(); i++) {
            int index = encoded.charAt(i);
            result.append(header.charAt(index));
        }
        
        return result.toString();
    }
    
    // 3. RLE с автоматическим определением порога
    private static String runLengthEmbedded(String input) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < input.length()) {
            char current = input.charAt(i);
            int count = 1;
            
            // Считаем повторы
            while (i + count < input.length() && input.charAt(i + count) == current) {
                count++;
            }
            
            if (count > 3) {
                // Кодируем как пару: символ + количество (как char)
                result.append("«").append(current).append((char) count).append("»");
                i += count;
            } else {
                // Просто копируем символы
                for (int j = 0; j < count; j++) {
                    result.append(current);
                }
                i += count;
            }
        }
        
        return "RLE|" + result.toString();
    }
    
    private static String extractRunLength(String embedded) {
        String data = embedded.substring(4); // Убираем "RLE|"
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < data.length(); i++) {
            if (data.charAt(i) == '«' && i + 2 < data.length()) {
                char ch = data.charAt(i + 1);
                int count = data.charAt(i + 2);
                for (int j = 0; j < count; j++) {
                    result.append(ch);
                }
                i += 3; // «, символ, количество, »
            } else {
                result.append(data.charAt(i));
            }
        }
        
        return result.toString();
    }
    
    // 4. Циклический паттерн (обнаружение и кодирование циклов)
    private static String patternCycleEmbedded(String input) {
        // Ищем самый длинный повторяющийся паттерн
        String bestPattern = "";
        int bestLength = 0;
        
        for (int patternLen = 1; patternLen <= input.length() / 2; patternLen++) {
            for (int start = 0; start <= input.length() - patternLen * 2; start++) {
                String pattern = input.substring(start, start + patternLen);
                int repeats = countRepeats(input, start, pattern);
                
                if (repeats > 1 && patternLen * repeats > bestLength) {
                    bestLength = patternLen * repeats;
                    bestPattern = pattern;
                }
            }
        }
        
        if (bestPattern.length() > 3) {
            // Кодируем: паттерн и сколько раз его повторить
            int totalRepeats = bestLength / bestPattern.length();
            return "CYC|" + bestPattern + "|" + totalRepeats + "|" + 
                   input.replace(bestPattern.repeat(totalRepeats), "");
        }
        
        return input;
    }
    
    private static int countRepeats(String input, int start, String pattern) {
        int count = 0;
        int pos = start;
        int patternLen = pattern.length();
        
        while (pos + patternLen <= input.length()) {
            if (input.substring(pos, pos + patternLen).equals(pattern)) {
                count++;
                pos += patternLen;
            } else {
                break;
            }
        }
        
        return count;
    }
    
    private static String extractCycle(String embedded) {
        String[] parts = embedded.split("\\|", 4);
        if (parts.length < 4) return embedded;
        
        String pattern = parts[1];
        int repeats = Integer.parseInt(parts[2]);
        String remainder = parts[3];
        
        return pattern.repeat(repeats) + remainder;
    }
    
    // Вспомогательный класс
    private static class Pair {
        char ch;
        int pos;
        
        Pair(char ch, int pos) {
            this.ch = ch;
            this.pos = pos;
        }
    }
    
    // Тест всех преобразований
    public static void testAll(String input) {
        System.out.println("=== САМООПИСЫВАЮЩИЕСЯ ПРЕОБРАЗОВАНИЯ ===");
        System.out.println("Исходные данные (первые 50): " + 
            input.substring(0, Math.min(50, input.length())));
        System.out.println("Длина: " + input.length());
        System.out.println();
        
        String[] transforms = {
            "SORT_EMBEDDED", 
            "FREQ_GROUP_EMBEDDED", 
            "RUN_LENGTH_EMBEDDED",
            "PATTERN_CYCLE_EMBEDDED"
        };
        
        for (String transform : transforms) {
            System.out.println("Преобразование: " + transform);
            String encoded = embedTransform(input, transform);
            String decoded = extractTransform(encoded);
            
            boolean reversible = input.equals(decoded);
            int encodedLength = encoded.length();
            
            System.out.printf("  Закодировано: %d символов (%.1f%% от исходного)%n",
                encodedLength, (encodedLength * 100.0 / input.length()));
            System.out.printf("  Обратимо: %s%n", reversible ? "✅" : "❌");
            
            if (reversible && encodedLength < input.length()) {
                System.out.printf("  ВЫИГРЫШ: %d символов (%.1f%%)%n",
                    input.length() - encodedLength,
                    (1.0 - (double)encodedLength / input.length()) * 100);
            }
            System.out.println();
        }
    }
}