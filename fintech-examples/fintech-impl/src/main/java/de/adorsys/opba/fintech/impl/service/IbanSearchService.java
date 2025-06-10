package de.adorsys.opba.fintech.impl.service;

import de.adorsys.opba.fintech.api.model.generated.InlineResponseBankInfo;
import de.adorsys.opba.fintech.impl.controller.utils.RestRequestContext;
import de.adorsys.opba.fintech.impl.mapper.BankInfoMapper;
import de.adorsys.opba.fintech.impl.tppclients.TppIbanSearchClient;
import de.adorsys.opba.tppbankingapi.bankinfo.model.generated.BankInfoResponse;
import de.adorsys.opba.tppbankingapi.bankinfo.model.generated.SearchBankinfoBody;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class IbanSearchService {

    private final TppIbanSearchClient tppIbanSearchClient;
    private final RestRequestContext restRequestContext;
    private final BankInfoMapper bankInfoMapper;

    @SneakyThrows
    public InlineResponseBankInfo searchByIban(String iban) {
        log.info("Searching for bank info by IBAN: {}", iban);
        UUID.fromString(restRequestContext.getRequestId());

        // Log the TPP Url being used
        log.info("TPP URL configured: ${tpp.url}");

        SearchBankinfoBody body = new SearchBankinfoBody();
        body.setIban(iban);

        log.info("Calling TPP Client with body: {}", body);

        // Get the full response first
        try {
            ResponseEntity<BankInfoResponse> fullResponse = tppIbanSearchClient.getBankInfoByIban(
                    UUID.fromString(restRequestContext.getRequestId()),  // xRequestID
                    body,                                                // SearchBankinfoBody
                    null,                                                // xTimestampUTC (optional)
                    null                                                 // fintechID (optional)
            );
            log.info("Full response status: {}, headers: {}", fullResponse.getStatusCode(), fullResponse.getHeaders());

            BankInfoResponse response = fullResponse.getBody();
            log.info("Response body: {}", response);

            if (response == null) {
                log.error("Received null response from TPP client for IBAN: {}", iban);
                return null;
            }

            InlineResponseBankInfo result = bankInfoMapper.mapFromTppToFintech(response);
            log.info("Mapped result: {}", result);

            return result;
        } catch (Exception e) {
            log.error("Error calling TPP Service: ", e);
            throw e;
        }
    }
}
