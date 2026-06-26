package local.contract.service;

import java.util.List;

import local.contract.model.ContractRequest;
import local.contract.model.ContractResponse;

/**
 * Service xử lý nghiệp vụ hợp đồng số hóa.
 * Gồm:
 * - Tạo hợp đồng tự động khi thanh toán thành công (qua MQ)
 * - Ký hợp đồng thủ công (người dùng ký)
 * - Lấy danh sách hợp đồng đã ký theo userId
 */
public interface ContractService {

    /**
     * 1️⃣ Người dùng ký hợp đồng thủ công (frontend gửi chữ ký base64).
     * @param request dữ liệu hợp đồng cần ký
     * @return phản hồi sau khi ký (ContractResponse)
     */
    ContractResponse signContract(ContractRequest request);

    /**
     * 2️⃣ Tạo hợp đồng tự động sau khi nhận MQ "order.paid"
     * @param request dữ liệu hợp đồng cần tạo
     * @return phản hồi (ContractResponse)
     */
    ContractResponse createContract(ContractRequest request);

    /**
     * 3️⃣ Lấy toàn bộ hợp đồng đã ký theo userId
     * @param userId ID của người dùng
     * @return danh sách hợp đồng (List<ContractResponse>)
     */
    List<ContractResponse> getContractsByUserId(Long userId);
}
