# 生成対象

backend/src/main/java/com/example/xapp/common/dto/CursorPage.java

# 制約

- package com.example.xapp.common.dto;
- Java 25 の record
- ジェネリック型パラメータ `<T>` を持つ
- 使ってよい import: java.util.List
- Lombok は使わない
- コメントは書かない

# 仕様

カーソルページングのレスポンスを表す汎用 record。

| フィールド | 型 | 説明 |
|---|---|---|
| items | List<T> | このページの要素 |
| nextCursor | String | 次ページ取得用のカーソル。次が無い場合は null |

コンパクトコンストラクタで、items が null の場合は空リストに置き換えること。

# 受け入れ条件

- `package com.example.xapp.common.dto;` から始まる
- `public record CursorPage<T>(List<T> items, String nextCursor)` のシグネチャである
- コンパイルが通る
