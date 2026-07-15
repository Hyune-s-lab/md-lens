# Mermaid Representative Patterns

Representative diagram types beyond flowcharts. The sequence and state diagrams pin a white
background through frontmatter `themeVariables.background`, which must survive the dark viewer
theme; the rest follow the viewer theme.

## Sequence — race condition (white canvas via frontmatter)

```mermaid
---
config:
  theme: base
  darkMode: false
  themeVariables:
    background: "#ffffff"
    primaryColor: "#eef2ff"
    primaryTextColor: "#111827"
    primaryBorderColor: "#475569"
    lineColor: "#334155"
---
sequenceDiagram
    participant A as 스레드 A
    participant B as 스레드 B
    participant S as stock = 1
    A->>S: read → 1
    B->>S: read → 1
    A->>S: write 1-1 = 0
    B->>S: write 1-1 = 0
    Note over B,S: 2건 주문됐는데 재고는 1만 감소 (lost update)
```

## State — order lifecycle (white canvas via frontmatter)

```mermaid
---
config:
  theme: base
  darkMode: false
  themeVariables:
    background: "#ffffff"
    primaryTextColor: "#111827"
    lineColor: "#334155"
---
stateDiagram-v2
    [*] --> PENDING
    PENDING --> CONFIRMED: pay
    CONFIRMED --> SHIPPED: ship
    CONFIRMED --> CANCELLED: cancel
    SHIPPED --> [*]
    CANCELLED --> [*]
```

## Class — domain model (viewer theme)

```mermaid
classDiagram
    class Order {
        +Long id
        +OrderStatus status
        +confirm() Order
    }
    class OrderLine {
        +Long id
        +int quantity
    }
    Order "1" *-- "many" OrderLine
```

## Gantt — release plan (viewer theme)

```mermaid
gantt
    title 0.4.0 Plan
    dateFormat YYYY-MM-DD
    section Renderer
        Syntax highlighting :done, 2026-07-14, 1d
        Highlight groups    :done, 2026-07-14, 1d
    section Editor
        Open as Text        :active, 2026-07-15, 2d
```

## Pie — traffic share (viewer theme)

```mermaid
pie title Requests by service
    "order" : 45
    "licensing" : 30
    "gateway" : 25
```
