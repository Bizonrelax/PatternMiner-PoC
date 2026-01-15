import java.util.*;

public class TransformLibrary {
    
    public static class TransformResult {
        public final String name;
        public final String transformed;
        public final int compressedSize;
        public final int extraDataSize; // Дополнительные данные для восстановления
        public final Map<String, Object> params;
        
        public TransformResult(String name, String transformed, int compressedSize, 
                              int extraDataSize, Map<String, Object> params) {
            this.name = name;
            this.transformed = transformed;
            this.compressedSize = compressedSize;
            this.extraDataSize = extraDataSize;
            this.params = params;
        }
        
        public int getTotalSize() {
            return compressedSize + extraDataSize;
        }
    }
    
    public static List<TransformResult> applyReversibleTransforms(String input, TransformCompressor compressor) {
        List<TransformResult> results = new ArrayList<>();
        
        // 1. BWT преобразование
        try {
            BWTTransformer.BWTResult bwtResult = BWTTransformer.forwardBWT(input);
            byte[] bwtCompressed = compressor.compressZip(bwtResult.transformed);
            // Для BWT нужно хранить индекс (4 байта) + возможно словарь
            results.add(new TransformResult("BWT", bwtResult.transformed, bwtCompressed.length, 4,
                Map.of("index", bwtResult.index)));
        } catch (Exception e) {
            System.out.println("BWT failed: " + e.getMessage());
        }
        
        // 2. Move-To-Front (MTF) после BWT
        try {
            BWTTransformer.BWTResult bwt = BWTTransformer.forwardBWT(input);
            String mtf = applyMTF(bwt.transformed);
            byte[] mtfCompressed = compressor.compressZip(mtf);
            // Нужно хранить: BWT-индекс (4 байта) + MTF-словарь (максимум 256 байт, но можно сжать)
            results.add(new TransformResult("BWT+MTF", mtf, mtfCompressed.length, 260,
                Map.of("bwtIndex", "needed", "mtfDict", "needed")));
        } catch (Exception e) {
            System.out.println("BWT+MTF failed: " + e.getMessage());
        }
        
        // 3. Обратимая сортировка с хранением перестановки
        String sortedAsc = sortWithPermutation(input, true);
        byte[] sortedCompressed = compressor.compressZip(sortedAsc);
        // Нужно хранить перестановку (индексы исходных позиций)
        // Для строки 112 символов - это 112 байт (можно сжать)
        results.add(new TransformResult("Сорт_с_перестановкой", sortedAsc, sortedCompressed.length, 112,
            Map.of("type", "sorted_asc_with_perm")));
        
        // 4. Обратимый XOR с известным ключом
        String xored = xorWithKey(input, 42); // ключ 42
        byte[] xoredCompressed = compressor.compressZip(xored);
        // Нужно хранить только ключ (1 байт)
        results.add(new TransformResult("XOR_ключ42", xored, xoredCompressed.length, 1,
            Map.of("key", 42)));
        
        // 5. Run-Length Encoding (RLE) - обратимое сжатие повторов
        String rleEncoded = simpleRLE(input);
        byte[] rleCompressed = compressor.compressZip(rleEncoded);
        // RLE сам по себе сжимает, но мы его ещё сжимаем ZIP'ом
        results.add(new TransformResult("RLE", rleEncoded, rleCompressed.length, 0,
            Map.of("type", "run_length")));
        
        return results;
    }
    
    private static String applyMTF(String input) {
        // Простая реализация Move-To-Front
        List<Character> alphabet = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            alphabet.add((char) i);
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            int index = alphabet.indexOf(c);
            result.append((char) index);
            // Перемещаем символ в начало
            alphabet.remove(index);
            alphabet.add(0, c);
        }
        return result.toString();
    }
    
    private static String sortWithPermutation(String input, boolean ascending) {
        // Создаем массив пар (символ, исходная позиция)
        List<CharWithIndex> chars = new ArrayList<>();
        for (int i = 0; i < input.length(); i++) {
            chars.add(new CharWithIndex(input.charAt(i), i));
        }
        
        // Сортируем
        if (ascending) {
            chars.sort(Comparator.comparingInt(a -> a.ch));
        } else {
            chars.sort((a, b) -> Character.compare(b.ch, a.ch));
        }
        
        // Собираем отсортированную строку
        StringBuilder sorted = new StringBuilder();
        for (CharWithIndex c : chars) {
            sorted.append(c.ch);
        }
        
        return sorted.toString();
        // Перестановка (индексы) должны храниться отдельно!
    }
    
    private static String xorWithKey(String input, int key) {
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            result.append((char) (c ^ key));
        }
        return result.toString();
    }
    
    private static String simpleRLE(String input) {
        StringBuilder result = new StringBuilder();
        if (input.isEmpty()) return "";
        
        char current = input.charAt(0);
        int count = 1;
        
        for (int i = 1; i < input.length(); i++) {
            if (input.charAt(i) == current && count < 255) {
                count++;
            } else {
                result.append(current);
                result.append((char) count);
                current = input.charAt(i);
                count = 1;
            }
        }
        result.append(current);
        result.append((char) count);
        
        return result.toString();
    }
    
    private static class CharWithIndex {
        final char ch;
        final int originalIndex;
        
        CharWithIndex(char ch, int originalIndex) {
            this.ch = ch;
            this.originalIndex = originalIndex;
        }
    }
}