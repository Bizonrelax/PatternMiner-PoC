import java.util.*;

public class BWTTransformer {
    
    // Прямое преобразование BWT
    public static BWTResult forwardBWT(String input) {
        int n = input.length();
        
        // Создаем все циклические сдвиги
        String[] rotations = new String[n];
        for (int i = 0; i < n; i++) {
            rotations[i] = input.substring(i) + input.substring(0, i);
        }
        
        // Сортируем сдвиги
        Arrays.sort(rotations);
        
        // Получаем последний столбец и индекс исходной строки
        StringBuilder lastColumn = new StringBuilder();
        int originalIndex = -1;
        for (int i = 0; i < n; i++) {
            lastColumn.append(rotations[i].charAt(n - 1));
            if (rotations[i].equals(input)) {
                originalIndex = i;
            }
        }
        
        return new BWTResult(lastColumn.toString(), originalIndex);
    }
    
    // Обратное преобразование BWT
    public static String inverseBWT(String lastColumn, int originalIndex) {
        int n = lastColumn.length();
        
        // Создаем список пар (символ, номер вхождения)
        List<BWTChar> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new BWTChar(lastColumn.charAt(i), i));
        }
        
        // Сортируем по символу (стабильная сортировка сохранит порядок вхождений)
        list.sort(Comparator.comparingInt(a -> a.ch));
        
        // Восстанавливаем исходную строку
        StringBuilder result = new StringBuilder();
        int currentIndex = originalIndex;
        
        for (int i = 0; i < n; i++) {
            BWTChar current = list.get(currentIndex);
            result.append(current.ch);
            currentIndex = current.originalPos;
        }
        
        return result.toString();
    }
    
    // Вспомогательный класс для BWT
    private static class BWTChar {
        final char ch;
        final int originalPos;
        
        BWTChar(char ch, int originalPos) {
            this.ch = ch;
            this.originalPos = originalPos;
        }
    }
    
    // Результат BWT преобразования
    public static class BWTResult {
        public final String transformed;
        public final int index;
        
        public BWTResult(String transformed, int index) {
            this.transformed = transformed;
            this.index = index;
        }
    }
}