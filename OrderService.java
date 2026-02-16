@RequiredArgsConstructor 
public class OrderService {
    private final OrderRepository repository;
    
    public Order getOrder(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException(id));
    }
}
