# Assignment 3 — Graph Algorithms (Java)

Готовое решение на Java 17: DSU, Kruskal, Prim, Kosaraju SCC, DFS topological sort,
DAG shortest paths и восстановление пути. Сторонние Java-библиотеки не нужны.

## Файлы

- `Algorithms.DSU` в `src/Java/Algorithms.java` — структура непересекающихся множеств. Сохранён твой конструктор;
  добавлены `find`, `union` и методы получения числа/размера компонент.
- `src/Java/Algorithms.java` — шесть методов и `reconstructPath`.
- `src/Java/Main.java`, `Input.java`, `Json.java` — неизменённый ввод/вывод официального шаблона.
- `tests/` — собственные проверки, входы, ожидаемые/фактические ответы и замеры.
- `ANALYSIS.md` — анализ на английском, корректность, сложности и результаты.
- `ASSIGNMENT.md`, `ASSIGNMENT_RU.md`, `docs/` — официальные требования.
- `notes/` — сохранённые первоначальные `Main.java` и пустой `test.java` как текст.
- Остальные языки в `src/` — неизменённые заготовки шаблона, не решение; выбран Java.

## Запуск в IntelliJ IDEA

1. Открой папку `Assignment3` как проект. Выбери установленный JDK 17 или новее
   в File → Project Structure → Project SDK. На этом компьютере проверен JDK 21.0.11.
2. Папка `src/Java` должна быть отмечена Sources Root (настройка внесена в `.iml`).
3. Запусти `main` в `src/Java/Main.java`.
4. Вставь в консоль одну строку запроса и нажми Enter:

```json
{"task":"dsu","n":5,"operations":[[0,1],[2,3],[1,3],[0,2]]}
```

Ответ:

```json
{"states":[[4,2],[3,2],[2,4],[2,4]]}
```

Программа затем ждёт следующий JSON-запрос. Это нормальное поведение: меню и
приглашения запрещены интерфейсом задания. Заверши ввод EOF или кнопкой Stop.
Для запуска тестов функции пути можно запустить `AlgorithmChecks.main` из `tests/java`.

## Сборка и проверки из корня проекта

Нужны JDK 17+ и Python 3.12+. Python запускает проверку Java-программы.

```text
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input tests/demo.jsonl
python tests/run_tests.py --scale --benchmark
```

На Windows вместо `python` можно использовать `py -3` или полный путь к Python.
Если системный `java` не запускается, укажи настоящий JDK перед командами:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11'
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
```

Ручная сборка и запуск в PowerShell:

```powershell
New-Item -ItemType Directory -Force .build/Java | Out-Null
javac --release 17 -encoding UTF-8 -d .build/Java src/Java/*.java
java -Xmx384m -cp .build/Java Main
```

## Проверенные результаты

- 36/36 публичных случаев; повтор двух публичных наборов в одном процессе.
- 513 собственных случаев, каждый выполнен дважды в одном процессе.
- 9 больших случаев: 100000 вершин и 250000 операций/рёбер каждый.
- Проверка путей, альтернативных корректных ответов и отклонения неверных результатов.
- По 5 измерений каждого алгоритма остова на трёх размерах графа.

Подробности, команды, ограничения измерений и раскрытие помощи AI — в `ANALYSIS.md`.

## Сдача

Создай свой GitHub-репозиторий через Use this template из официального шаблона,
перенеси туда содержимое этой папки, создай коммит и отправь изменения.
Дождись зелёного Actions → Public checks. В Moodle укажи ссылку на конкретный коммит
и нажми Submit assignment. Локальные проверки уже выполнены; отправка на GitHub и
в Moodle из этой папки ещё не выполнялась.
