package local.wallet_service.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import local.wallet_service.service.WalletService;

@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final WalletService walletService;

    @GetMapping("/balance")
    public ResponseEntity<?> getPlatformBalance() {
        return ResponseEntity.ok(walletService.getPlatformBalance());
    }
}
