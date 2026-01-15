public class TestData {
    // Исходный тест
    public static final String TEST_BASE64 = 
        "H4sIAAAAAAAA/6tWSs7PS8tJLVayUspIzcnJV0pLzClOtVJKLEnMTVWyUiouKcrMS0ksSS1SslJKTMvNz0vRy0ksSS0CCzWkVgMAQNExR1AAAAA=";
    
    // Большие данные (в 16 раз больше)
    public static final String LARGER_BASE64;
    
    // Очень большие данные с разными паттернами
    public static final String HUGE_DATA;
    
    static {
        // Создаем большую строку с паттернами
        StringBuilder large = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            large.append(TEST_BASE64);
        }
        LARGER_BASE64 = large.toString();
        
        // Создаем данные с явными паттернами
        StringBuilder huge = new StringBuilder();
        // Паттерн 1: Много 'A'
        for (int i = 0; i < 100; i++) huge.append('A');
        // Паттерн 2: Чередование
        for (int i = 0; i < 50; i++) huge.append("XYZ");
        // Паттерн 3: Повторы
        for (int i = 0; i < 30; i++) huge.append("1234567890");
        // Случайные данные
        huge.append(TEST_BASE64).append(TEST_BASE64);
        HUGE_DATA = huge.toString();
    }
}