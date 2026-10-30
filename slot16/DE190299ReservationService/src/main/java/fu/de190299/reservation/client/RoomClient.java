package fu.de190299.reservation.client;

import fu.de190299.reservation.dto.ApiResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// [KHAC BAI 1]: Feign Client goi sang RoomService de lay thong tin phong
@FeignClient(name = "room-service", url = "${room.service.url}")
public interface RoomClient {

    @GetMapping("/api/rooms/{roomId}")
    ApiResponseDTO getRoomById(@PathVariable("roomId") Long roomId);
}
