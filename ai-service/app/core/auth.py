"""
JWT 身份认证模块
----------------
从 Authorization 请求头中解析 JWT Token，验证签名并取出用户 ID。
与 Spring Boot 后端共享同一个 JWT secret，实现跨服务无状态认证。
"""

import jwt
from fastapi import Header, HTTPException, status
from app.core.config import settings


def get_current_user_id(authorization: str | None = Header(default=None)) -> int:
    """
    从 Authorization 头解析 JWT，返回当前用户 ID。

    Token 格式：Authorization: Bearer <jwt>
    失败时抛出 401 未授权异常。
    """
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="缺少有效的 Authorization 头",
            headers={"WWW-Authenticate": "Bearer"},
        )

    token = authorization[len("Bearer "):].strip()
    try:
        payload = jwt.decode(
            token,
            settings.JWT_SECRET,
            algorithms=[settings.JWT_ALGORITHM],
        )
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="登录已过期，请重新登录",
        )
    except jwt.InvalidTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="无效的登录凭证",
        )

    subject = payload.get("sub")
    if not subject:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token 中缺少用户标识",
        )

    try:
        return int(subject)
    except (TypeError, ValueError):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="用户标识格式错误",
        )