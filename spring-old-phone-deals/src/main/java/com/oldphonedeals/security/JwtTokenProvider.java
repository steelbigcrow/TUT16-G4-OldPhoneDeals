package com.oldphonedeals.security;

import com.oldphonedeals.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 提供者
 * <p>
 * 负责 JWT Token 的生成、解析和验证。使用 HS512 算法对 Token 进行签名，
 * 确保 Token 的完整性和真实性。与 Express.js 后端的 JWT 实现保持兼容。
 * </p>
 * <p>
 * Token 格式：
 * - Header: {"alg": "HS512", "typ": "JWT"}
 * - Payload: {"sub": userId, "email": userEmail, "isAdmin": true/false, "iat": ..., "exp": ...}
 * - Signature: HMACSHA512(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
 * </p>
 *
 * @author OldPhoneDeals Team
 */
@Slf4j
@Component
public class JwtTokenProvider {

  /**
   * JWT 密钥（从配置文件读取，需要是 Base64 编码的至少 256 位密钥）
   */
  @Value("${jwt.secret}")
  private String jwtSecret;

  /**
   * JWT 过期时间（毫秒）
   * 默认：604800000ms = 7天
   */
  @Value("${jwt.expiration:604800000}")
  private long jwtExpiration;

  /**
   * 解析后的密钥对象
   */
  private SecretKey key;

  /**
   * 初始化密钥
   * <p>
   * 在 Bean 创建后自动调用，将 Base64 编码的密钥字符串解析为 SecretKey 对象
   * </p>
   */
  @PostConstruct
  public void init() {
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    log.info("JWT Token Provider initialized with HS512 algorithm");
  }

  /**
   * 生成 JWT Token
   * <p>
   * 创建包含用户信息的 JWT Token，与 Express.js 的实现保持兼容。
   * Token 中包含以下 claims：
   * - sub: 用户 ID
   * - email: 用户邮箱
   * - isAdmin: 是否为管理员
   * </p>
   *
   * @param user 用户实体对象
   * @return JWT Token 字符串
   */
  public String generateToken(User user) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpiration);

    Map<String, Object> claims = new HashMap<>();
    claims.put("email", user.getEmail());
    claims.put("role", user.getRole() != null ? user.getRole() : "USER");
    // 保留isAdmin以保持向后兼容
    claims.put("isAdmin", "ADMIN".equals(user.getRole()) || user.getIsAdmin());

    String token = Jwts.builder()
        .subject(user.getId())
        .claims(claims)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(key, Jwts.SIG.HS512)
        .compact();

    log.debug("Generated JWT token for user: {}", user.getEmail());
    return token;
  }

  /**
   * 从 Token 提取用户 ID
   *
   * @param token JWT Token 字符串
   * @return 用户 ID
   */
  public String getUserIdFromToken(String token) {
    Claims claims = parseToken(token);
    return claims.getSubject();
  }

  /**
   * 从 Token 提取用户邮箱
   *
   * @param token JWT Token 字符串
   * @return 用户邮箱
   */
  public String getEmailFromToken(String token) {
    Claims claims = parseToken(token);
    return claims.get("email", String.class);
  }

  /**
   * 验证 Token 的有效性
   * <p>
   * 检查 Token 的签名、过期时间等。如果 Token 无效、过期或被篡改，
   * 则返回 false 并记录相应的错误日志。
   * </p>
   *
   * @param token JWT Token 字符串
   * @return Token 是否有效
   */
  public boolean validateToken(String token) {
    try {
      parseToken(token);
      return true;
    } catch (SignatureException ex) {
      log.error("Invalid JWT signature: {}", ex.getMessage());
    } catch (MalformedJwtException ex) {
      log.error("Invalid JWT token: {}", ex.getMessage());
    } catch (ExpiredJwtException ex) {
      log.error("Expired JWT token: {}", ex.getMessage());
    } catch (UnsupportedJwtException ex) {
      log.error("Unsupported JWT token: {}", ex.getMessage());
    } catch (IllegalArgumentException ex) {
      log.error("JWT claims string is empty: {}", ex.getMessage());
    }
    return false;
  }

  /**
   * 解析 Token 获取 Claims
   *
   * @param token JWT Token 字符串
   * @return Claims 对象
   * @throws JwtException 如果 Token 无效
   */
  private Claims parseToken(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}