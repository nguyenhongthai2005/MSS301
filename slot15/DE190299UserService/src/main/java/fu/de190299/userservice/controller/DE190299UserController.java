package fu.de190299.userservice.controller;

import fu.de190299.userservice.dto.ApiResponse;
import fu.de190299.userservice.dto.UserDTO;
import fu.de190299.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class DE190299UserController {

    private final UserService userService;

    public DE190299UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createUser(@Valid @RequestBody UserDTO dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation failed");
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, errorMsg));
        }
        ApiResponse res = userService.createUser(dto);
        return map(res, HttpStatus.CREATED);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse> updateUser(@PathVariable Long userId, @RequestBody UserDTO dto) {
        ApiResponse res = userService.updateUser(userId, dto);
        return map(res, HttpStatus.OK);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long userId) {
        ApiResponse res = userService.getUserById(userId);
        return map(res, HttpStatus.OK);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse> deactivateUser(@PathVariable Long userId) {
        ApiResponse res = userService.deactivateUser(userId);
        return map(res, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getUserList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        if (size < 1 || size > 100 || page < 0) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(ApiResponse.error(2, "Validation failed: size must be between 1 and 100, page >= 0"));
        }
        ApiResponse res = userService.getUserList(role, status, PageRequest.of(page, size));
        return map(res, HttpStatus.OK);
    }

    private ResponseEntity<ApiResponse> map(ApiResponse res, HttpStatus successStatus) {
        return switch (res.getStatus()) {
            case 1 -> ResponseEntity.status(successStatus).body(res);
            case 2 -> ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(res); // 406
            case 3 -> ResponseEntity.status(226).body(res);                       // 226 IM Used
            case 4 -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);        // 404
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res); // 500
        };
    }
}
