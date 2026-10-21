package com.idet.pos.batch;

import com.idet.pos.dto.ItemDTO;
import com.idet.pos.entity.Inventory;
import com.idet.pos.entity.Item;
import com.idet.pos.repository.ItemRepository;
import jakarta.validation.Validator;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.validator.ValidationException;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates one item CSV row and builds a new Item with its opening stock.
 * Rows whose code already exists in the database, or appeared earlier in the same file, are filtered out,
 * so the import only inserts new items. Step-scoped, so the duplicate tracking starts fresh for every run.
 */
public class ItemImportProcessor implements ItemProcessor<ItemCsvRow, Item> {

    private final ItemRepository itemRepository;
    private final Validator validator;
    private final Set<String> seenCodes = new HashSet<>();

    public ItemImportProcessor(ItemRepository itemRepository, Validator validator) {
        this.itemRepository = itemRepository;
        this.validator = validator;
    }

    @Override
    public Item process(ItemCsvRow row) {
        ItemDTO dto = new ItemDTO(row.code(), row.description(), row.unitPrice());
        ImportValidation.validate(validator, dto, row.code());
        if (row.qty() < 0) {
            throw new ValidationException("Invalid row " + row.code() + " - qty: must be zero or positive");
        }

        if (!seenCodes.add(row.code()) || itemRepository.existsById(row.code())) {
            return null;
        }

        Item item = new Item(row.code(), row.description(), row.unitPrice());
        item.setInventory(new Inventory(row.code(), row.qty(), item));
        return item;
    }
}
