package br.com.basis.abaco.security.jwt;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.WebUtils;

import br.com.basis.abaco.security.AuthenticationConstants;
import io.jsonwebtoken.ExpiredJwtException;

/**
 * Filters incoming requests and installs a Spring Security principal if a
 * header corresponding to a valid user is found.
 */
public class JWTFilter extends GenericFilterBean {

	private final Logger log = LoggerFactory.getLogger(JWTFilter.class);

	private TokenProvider tokenProvider;

	// TODO injetar CookieUtil sem construtor?
	// via construtor implica em 3 classes +- injetando via construtor também
	// private CookieUtil cookieUtil;

	public JWTFilter(TokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		try {
			HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
			String jwt = resolveToken(httpServletRequest);
			if (StringUtils.hasText(jwt) && this.tokenProvider.validateToken(jwt)) {
				Authentication authentication = this.tokenProvider.getAuthentication(jwt);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
			filterChain.doFilter(servletRequest, servletResponse);
		} catch (ExpiredJwtException eje) {
			log.info("Security exception for user {} - {}", eje.getClaims().getSubject(), eje.getMessage());

			log.trace("Security exception trace: {}", eje);
			((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		}
	}

	private String resolveToken(HttpServletRequest request) {
		try {
			return doResolveToken(request);
		} catch (Throwable e) {
			return null;
		}
	}

	private String doResolveToken(HttpServletRequest request) {
		if (requestHasAuthenticationCookie(request))
			return resolveByCookie(request);
		else
			return resolveTokenByHeader(request);
	}

	private boolean requestHasAuthenticationCookie(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, AuthenticationConstants.TOKEN_NAME);
		return cookie != null;
	}

	private String resolveByCookie(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, AuthenticationConstants.TOKEN_NAME);
		return cookie != null ? cookie.getValue() : null;
	}

	private String resolveTokenByHeader(HttpServletRequest request) {
		String bearerToken = request.getHeader(JWTConfigurer.AUTHORIZATION_HEADER);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7, bearerToken.length());
		}
		return null;
	}

}
