package kh.karazin.foodwise.store.adapter.in.rest;

import kh.karazin.foodwise.common.response.ApiResponse;
import kh.karazin.foodwise.store.application.port.in.StoreUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.method;
import static org.springframework.web.servlet.function.RequestPredicates.path;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Exposes store search over the HTTP {@code QUERY} method as a body-carrying
 * alternative to {@code GET /stores}.
 *
 * <p>{@code QUERY} is not part of {@code RequestMethod}, so it cannot be mapped
 * with {@code @GetMapping}/{@code @RequestMapping}. Since Spring Framework 6
 * {@code HttpMethod} is an open class rather than an enum, and {@code WebMvc.fn}
 * matches on it directly — {@code DispatcherServlet} hands every method to the
 * handler mapping, and {@code FrameworkServlet} already lets non-standard
 * methods through. A functional route is therefore the clean way to serve it.
 *
 * <p>The same {@link StoreUseCase} that backs {@code GET /stores} is reused
 * unchanged, and a filter translates the port's {@link IllegalArgumentException}
 * (unknown sort, {@code type}/{@code group} conflict) into the same 400
 * {@link ApiResponse} envelope the annotated controller returns.
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
class StoreQueryRouterConfig {

    static final HttpMethod QUERY = HttpMethod.valueOf("QUERY");

    private final StoreUseCase storeUseCase;
    private final StoreRestMapper storeRestMapper;

    @Bean
    RouterFunction<ServerResponse> storeQueryRoutes() {
        return route()
                .route(method(QUERY).and(path("/stores")), this::searchStores)
                .filter(this::translateBadRequest)
                .build();
    }

    private ServerResponse searchStores(ServerRequest request) throws Exception {
        StoreSearchQuery query = request.body(StoreSearchQuery.class);
        Page<StoreDto> result = storeUseCase.searchStores(query.toParams()).map(storeRestMapper::toDto);
        return ServerResponse.ok().body(ApiResponse.success(result));
    }

    private ServerResponse translateBadRequest(ServerRequest request,
                                               HandlerFunction<ServerResponse> next) throws Exception {
        try {
            return next.handle(request);
        } catch (IllegalArgumentException e) {
            // Unknown sort, type/group conflict — echo the domain message.
            return ServerResponse.badRequest().body(ApiResponse.error("BAD_REQUEST", e.getMessage()));
        } catch (HttpMessageNotReadableException e) {
            // Malformed/empty JSON body. Do not echo the parser message (may contain
            // the payload); keep the same envelope the annotated controller returns.
            return ServerResponse.badRequest().body(ApiResponse.error("BAD_REQUEST", "Malformed or invalid request body"));
        }
    }
}
