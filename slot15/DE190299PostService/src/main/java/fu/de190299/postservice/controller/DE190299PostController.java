package fu.de190299.postservice.controller;

import fu.de190299.postservice.dto.ApiResponse;
import fu.de190299.postservice.dto.PostDTO;
import fu.de190299.postservice.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class DE190299PostController {

    private final PostService postService;

    public DE190299PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createPost(@Valid @RequestBody PostDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation failed");
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, errorMsg));
        }
        ApiResponse res = postService.createPost(dto);
        return map(res, HttpStatus.CREATED);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse> updatePost(@PathVariable Long postId, @RequestBody PostDTO dto) {
        ApiResponse res = postService.updatePost(postId, dto);
        return map(res, HttpStatus.OK);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse> getPostById(@PathVariable Long postId) {
        ApiResponse res = postService.getPostById(postId);
        return map(res, HttpStatus.OK);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse> cancelPost(@PathVariable Long postId) {
        ApiResponse res = postService.cancelPost(postId);
        return map(res, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getPostList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String advertiserName,
            @RequestParam(required = false) String status) {
        if (size < 1 || size > 100 || page < 0) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, "Validation failed: size must be between 1 and 100, page >= 0"));
        }
        ApiResponse res = postService.getPostList(advertiserName, status, PageRequest.of(page, size));
        return map(res, HttpStatus.OK);
    }

    private ResponseEntity<ApiResponse> map(ApiResponse res, HttpStatus successStatus) {
        return switch (res.getStatus()) {
            case 1 -> ResponseEntity.status(successStatus).body(res);
            case 2 -> ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(res); // 406
            case 3 -> ResponseEntity.status(226).body(res);                       // 226
            case 4 -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);        // 404
            case 5 -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);      // 400 User not ACTIVE
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res); // 500
        };
    }
}
