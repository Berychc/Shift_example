package com.example;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Утилита для фильтрации данных из файлов.
 * Класс читает данные из входных файлов, классифицирует их на целые числа, вещественные числа и строки,
 * и записывает результаты в выходные файлы.
 */
public class FileFilteringUtil {

    private static final String IS_NUM_STRING = "[-+]?\\d+";
    private static final String IS_NUM_STRING_MATCHES = "[-+]?\\d*\\.\\d+([eE][-+]?\\d+)?";

    /**
     * Точка входа в программу. Обрабатывает аргументы командной строки,
     * читает данные из входных файлов, классифицирует их и записывает в выходные файлы.
     *
     * @param args аргументы командной строки, включая названия входных файлов и опции
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Не предоставлены входные файлы.");
            return;
        }

        String outputDirectory = "."; // Директория по умолчанию
        String prefix = ""; // Префикс по умолчанию
        boolean append = false;
        boolean fullStats = false;

        List<String> inputFiles = new ArrayList<>();

        // Обработка аргументов командной строки
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o":
                    outputDirectory = getNextArg(args, i++);
                    break;
                case "-p":
                    prefix = getNextArg(args, i++);
                    break;
                case "-a":
                    append = true;
                    break;
                case "-s":
                    fullStats = false;
                    break;
                case "-f":
                    fullStats = true;
                    break;
                default:
                    if (!args[i].startsWith("-")) {
                        inputFiles.add(args[i]);
                    }
                    break;
            }
        }

        // Создание выходной директории, если она не существует
        createDirectoryIfNotExists(outputDirectory);

        // Списки для хранения данных
        List<Long> integers = new ArrayList<>();
        List<Double> floats = new ArrayList<>();
        List<String> strings = new ArrayList<>();

        for (String filePath : inputFiles) {
            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = br.readLine()) != null) {
                    classifyInformation(line, integers, floats, strings);
                }
            } catch (IOException e) {
                System.out.printf("Ошибка чтения файла %s: %s%n", filePath, e.getMessage());
            }
        }

        // Сортировка строк в алфавитном порядке
        Collections.sort(strings);

        // Запись данных в выходные файлы
        writeRecord(outputDirectory, prefix, "integers.txt", integers, append);
        writeRecord(outputDirectory, prefix, "floats.txt", floats, append);
        writeRecord(outputDirectory, prefix, "strings.txt", strings, append);

        // Вывод статистики
        printFileContents(integers, floats, strings, fullStats);
    }

    /**
     * Получает следующий аргумент из массива аргументов командной строки.
     *
     * @param args массив аргументов командной строки
     * @param currentIndex текущий индекс в массиве аргументов командной строки
     * @return следующий аргумент или пустую строку, если его нет
     */
    private static String getNextArg(String[] args, int currentIndex) {
        return (currentIndex + 1 < args.length) ? args[currentIndex + 1] : "";
    }

    /**
     * Классифицирует строку текста на целые числа, вещественные числа и строки.
     *
     * @param line строка текста для классификации
     * @param integers список для хранения классифицированных целых чисел
     * @param floats список для хранения классифицированных вещественных чисел
     * @param strings список для хранения классифицированных строк
     */
    private static void classifyInformation(String line, List<Long> integers, List<Double> floats, List<String> strings) {
        line = line.trim();
        if (line.isEmpty()) {
            return;
        }
        try {
            if (line.matches(IS_NUM_STRING)) {
                Long value = Long.valueOf(line);
                integers.add(value);
            } else if (line.matches(IS_NUM_STRING_MATCHES)) {
                floats.add(Double.valueOf(line));
            } else {
                strings.add(line); // Если это не число, добавляем в строки
            }
        } catch (NumberFormatException e) {
            strings.add(line);
        }
    }

    /**
     * Записывает записи в указанный файл в выходной директории.
     *
     * @param outputDirectory директория для записи выходного файла
     * @param prefix префикс, который будет добавлен к названию выходного файла
     * @param fileName имя выходного файла
     * @param data данные для записи в выходной файл
     * @param append флаг, указывающий, нужно ли добавлять данные в файл, если он уже существует
     */
    private static void writeRecord(String outputDirectory, String prefix, String fileName, List<?> data, boolean append) {
        if (data.isEmpty()) return;

        if (data instanceof List && !data.isEmpty() && data.get(0) instanceof Double) {
            Collections.sort((List<Double>) data, Comparator.reverseOrder()); // Сортировка по убыванию
        }

        Path outputFilePath = Paths.get(outputDirectory, prefix + fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath, append ? StandardOpenOption.APPEND : StandardOpenOption.CREATE)) {
            for (Object item : data) {
                writer.write(item.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.printf("Ошибка записи в файл %s: %s%n", outputFilePath, e.getMessage());
        }
    }

    /**
     * Выводит содержимое и статистику классифицированных данных.
     *
     * @param integers список классифицированных целых чисел
     * @param floats список классифицированных вещественных чисел
     * @param strings список классифицированных строк
     * @param fullStats флаг, указывающий, нужно ли выводить полную статистику
     */
    private static void printFileContents(List<Long> integers, List<Double> floats, List<String> strings, boolean fullStats) {
        System.out.println("Статистика:");

        if (!integers.isEmpty()) {
            System.out.println("Количество целых чисел: " + integers.size());
            if (fullStats) {
                System.out.println("Минимум: " + Collections.min(integers));
                System.out.println("Максимум: " + Collections.max(integers));
                System.out.println("Сумма: " + integers.stream().mapToLong(Long::longValue).sum());
                System.out.println("Среднее: " + integers.stream().mapToDouble(Long::doubleValue).average().orElse(0));
            }
        }

        if (!floats.isEmpty()) {
            System.out.println("Количество вещественных чисел: " + floats.size());
            if (fullStats) {
                System.out.println("Минимум: " + Collections.min(floats));
                System.out.println("Максимум: " + Collections.max(floats));
                System.out.println("Сумма: " + floats.stream().mapToDouble(Double::doubleValue).sum());
                System.out.println("Среднее: " + floats.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
            }
        }

        if (!strings.isEmpty()) {
            System.out.println("Количество строк: " + strings.size());
            if (fullStats) {
                System.out.println("Кратчайшая строка: " + strings.stream().mapToInt(String::length).min().orElse(0));
                System.out.println("Длиннейшая строка: " + strings.stream().mapToInt(String::length).max().orElse(0));
            }
        }
    }

    /**
     * Создает директорию, если она еще не существует.
     *
     * @param directory директория для создания
     */
    private static void createDirectoryIfNotExists(String directory) {
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}