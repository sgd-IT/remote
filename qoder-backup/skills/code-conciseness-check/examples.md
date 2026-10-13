# Code Conciseness Check - Examples

## 1. Duplicate Code

**Before:**
```java
public void processUser(User user) {
    if (user == null) {
        log.error("User is null");
        throw new IllegalArgumentException("User cannot be null");
    }
    // ... user processing
}

public void updateUser(User user) {
    if (user == null) {
        log.error("User is null");
        throw new IllegalArgumentException("User cannot be null");
    }
    // ... user update
}
```

**After:**
```java
private void validateUser(User user) {
    if (user == null) {
        log.error("User is null");
        throw new IllegalArgumentException("User cannot be null");
    }
}

public void processUser(User user) {
    validateUser(user);
    // ... user processing
}

public void updateUser(User user) {
    validateUser(user);
    // ... user update
}
```

## 2. Overly Long Methods

**Before** (60+ lines with multiple responsibilities):
```java
public Result processData(Request req) {
    // validate request (10 lines)
    // fetch data (10 lines)
    // transform data (15 lines)
    // save results (10 lines)
    // send notification (10 lines)
    // return result (5 lines)
}
```

**After** (each method < 20 lines, single responsibility):
```java
public Result processData(Request req) {
    validateRequest(req);
    Data data = fetchData(req);
    Data transformed = transformData(data);
    saveResults(transformed);
    sendNotification(transformed);
    return buildResult(transformed);
}
```

## 3. Unnecessary Complexity

**Before:**
```java
public String getStatus(Order order) {
    String status;
    if (order.isPaid()) {
        if (order.isShipped()) {
            if (order.isDelivered()) {
                status = "DELIVERED";
            } else {
                status = "SHIPPED";
            }
        } else {
            status = "PAID";
        }
    } else {
        status = "PENDING";
    }
    return status;
}
```

**After (guard clauses):**
```java
public String getStatus(Order order) {
    if (!order.isPaid()) return "PENDING";
    if (!order.isShipped()) return "PAID";
    if (!order.isDelivered()) return "SHIPPED";
    return "DELIVERED";
}
```

## 4. Redundant Conditional Logic

**Before:**
```java
public boolean isActive(User user) {
    if (user.getStatus() == Status.ACTIVE) {
        return true;
    } else {
        return false;
    }
}
```

**After:**
```java
public boolean isActive(User user) {
    return user.getStatus() == Status.ACTIVE;
}
```

**Before:**
```java
boolean flag = condition ? true : false;
```

**After:**
```java
boolean flag = condition;
```

## 5. Simplifiable Expressions

**Before:**
```java
List<String> result = new ArrayList<>();
for (Item item : items) {
    if (item.isValid()) {
        result.add(item.getName());
    }
}
```

**After (Stream API):**
```java
List<String> result = items.stream()
    .filter(Item::isValid)
    .map(Item::getName)
    .collect(Collectors.toList());
```

**Before:**
```java
String result = "";
for (String s : list) {
    result += s;
}
```

**After:**
```java
String result = String.join("", list);
```

## 6. Dead Code

**Before:**
```java
public void process(String input) {
    if (input == null) {
        throw new IllegalArgumentException("Input cannot be null");
        log.info("Processing null input"); // Dead code - unreachable
    }
    // ...
}

// Unused private method
private void oldProcess(String input) {
    // legacy implementation
}
```

**After:**
```java
public void process(String input) {
    if (input == null) {
        throw new IllegalArgumentException("Input cannot be null");
    }
    // ...
}
```
