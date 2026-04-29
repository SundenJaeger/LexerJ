# LexerJ

LexerJ is a pure interpreter for the LEXOR programming language — a strongly-typed language designed to teach Senior
High School students the fundamentals of programming.

## Download

Grab the latest release from the [releases](https://github.com/SundenJaeger/LexerJ/releases) tab.

### Requirements

- **Java 25** or later

## Usage

Run the program directly from the terminal:

**Using the JAR:**

```shell
java -jar LexerJ.jar <path/to/file>
```

**Using the EXE:**

```shell
LexerJ.exe <path/to/file>
```

Or drag and drop your source file onto `LexerJ.exe`

## Language Overview

### Program Structure

Every LEXOR program follows this structure:

```
%% this is a comment
SCRIPT AREA
START SCRIPT
    DECLARE INT x=10
    DECLARE CHAR a='z'
    %% executable code goes here
    PRINT: x & $ & a
END SCRIPT
```

- All programs begin with `SCRIPT AREA`
- All code is placed inside `START SCRIPT` and `END SCRIPT`
- Variable declarations must come **immediately after** `START SCRIPT`
- Each line contains a single statement
- Comments start with `%%` and can be placed anywhere
- Reserved words are always **CAPITAL LETTERS**

### Data Types

| Type    | Description                      |
|---------|----------------------------------|
| `INT`   | Whole number, occupies 4 bytes   |
| `FLOAT` | Decimal number, occupies 4 bytes |
| `CHAR`  | A single character               |
| `BOOL`  | `"TRUE"` or `"FALSE"` literal    |

Variable names are **case-sensitive** and must start with a letter or underscore (`_`), followed by letters,
underscores, or digits.

### Declaring Variables

```
DECLARE INT count=0
DECLARE FLOAT price=9.99
DECLARE CHAR grade='A'
DECLARE BOOL passed="TRUE"
```

### Operators

**Arithmetic** (in order of precedence):

```
( )          parenthesis
*, /, %      multiplication, division, modulo
+, -         addition, subtraction
```

**Comparison:**

```
>, <         greater than, less than
>=, <=       greater than or equal, less than or equal
==, <>       equal, not equal
```

**Logical** (operate on `BOOL` expressions):

```
AND          both expressions must be true
OR           at least one expression must be true
NOT          reverses the boolean value
```

**Unary:**

```
+   positive
-   negative
```

### Output — `PRINT`

`PRINT` writes to the output. Use `&` to concatenate values, `$` for a newline, and `[#]` as an escape code for `#`.

```
PRINT: x & " is the value" & $ & "next line"
```

Square brackets `[ ]` are used for escape codes, e.g. `[[]` outputs a literal `[`.

### Input — `SCAN`

`SCAN` reads user input into one or more variables, separated by commas.

```
SCAN: x, y
```

The user types values separated by commas at the prompt.

### Control Flow

#### If

```
IF (<BOOL expression>)
START IF
    <statement>
END IF
```

#### If-Else

```
IF (<BOOL expression>)
START IF
    <statement>
END IF
ELSE
START IF
    <statement>
END IF
```

#### If-Else If-Else

```
IF (<BOOL expression>)
START IF
    <statement>
END IF
ELSE IF (<BOOL expression>)
START IF
    <statement>
END IF
ELSE
START IF
    <statement>
END IF
```

#### For Loop

```
FOR (<initialization>, <condition>, <update>)
START FOR
    <statement>
END FOR
```

#### Repeat (While) Loop

```
REPEAT WHEN (<BOOL expression>)
START REPEAT
    <statement>
END REPEAT
```

## Sample Programs

> Check the [tests](https://github.com/SundenJaeger/LexerJ/tree/master/tests) folder for sample programs

## Contributing

- Fork the repo
- Create a feature branch
- Commit with clear messages
- Open a PR with a concise description and screenshots/gifs if UI is affected
