package fu.de190299.room.controller;

import fu.de190299.room.dto.ApiResponseDTO;
import fu.de190299.room.dto.RoomDTO;
import fu.de190299.room.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class DE190299RoomController {

    private final RoomService roomService;

    public DE190299RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO> createRoom(@Valid @RequestBody RoomDTO dto) {
        ApiResponseDTO res = roomService.createRoom(dto);
        return toResponseEntity(res, HttpStatus.CREATED);
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<ApiResponseDTO> updateRoom(@PathVariable("roomId") Long roomId,
                                                     @RequestBody RoomDTO dto) {
        ApiResponseDTO res = roomService.updateRoom(roomId, dto);
        return toResponseEntity(res, HttpStatus.OK);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponseDTO> getRoomDetail(@PathVariable("roomId") Long roomId) {
        ApiResponseDTO res = roomService.getRoomById(roomId);
        return toResponseEntity(res, HttpStatus.OK);
    }

    // [KHAC BAI 1]: DELETE la Soft-Update sang MAINTENANCE (tra ve 200 OK)
    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponseDTO> setMaintenance(@PathVariable("roomId") Long roomId) {
        ApiResponseDTO res = roomService.setMaintenance(roomId);
        return toResponseEntity(res, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO> getRooms(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "roomType", required = false) String roomType,
            @RequestParam(name = "status", required = false) String status) {
        ApiResponseDTO res = roomService.getRooms(page, size, roomType, status);
        return toResponseEntity(res, HttpStatus.OK);
    }

    // [KHAC BAI 1]: Mapping status sang HTTP status code theo yeu cau de thi:
    // status = 1 -> HttpStatus.OK / CREATED
    // status = 2 -> HttpStatus.NOT_ACCEPTABLE (406)
    // status = 3 -> HttpStatus.IM_USED (226) - Room number is duplicated (bai 1 la status = 2)
    // status = 4 -> HttpStatus.NOT_FOUND (404)
    // status = 0 -> HttpStatus.INTERNAL_SERVER_ERROR (500)
    private ResponseEntity<ApiResponseDTO> toResponseEntity(ApiResponseDTO res, HttpStatus successStatus) {
        if (res.getStatus() == 1) {
            return ResponseEntity.status(successStatus).body(res);
        }
        if (res.getStatus() == 2) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(res);
        }
        if (res.getStatus() == 3) {
            return ResponseEntity.status(HttpStatus.IM_USED).body(res); // 226 duplicate roomNumber
        }
        if (res.getStatus() == 4) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res); // 404
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res); // 500
    }
}
