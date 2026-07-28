package com.example.xapp.common;

import com.example.xapp.common.exception.InvalidCursorException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/**
 * カーソルの符号化。中身は UUID だが、<strong>クライアントから見て不透明な文字列</strong>として
 * 扱う（docs/adr/0003）。
 *
 * <p>不透明にしておくことで、将来ソートキーを変えたり fan-out on write に切り替えたりしても
 * クライアントを壊さずに実装を差し替えられる。
 */
public final class CursorCodec {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private CursorCodec() {}

    public static String encode(UUID id) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(id.getMostSignificantBits());
        buffer.putLong(id.getLeastSignificantBits());
        return ENCODER.encodeToString(buffer.array());
    }

    /**
     * @param cursor null または空文字なら「先頭から」を意味する null を返す
     * @throws InvalidCursorException 復号できない場合（400 を返すため）
     */
    public static UUID decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = DECODER.decode(cursor);
            if (bytes.length != 16) {
                throw new InvalidCursorException();
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        } catch (IllegalArgumentException e) {
            throw new InvalidCursorException();
        }
    }
}
