ALTER TABLE request_note
    ADD CONSTRAINT fk_request_note_author FOREIGN KEY (author_id) REFERENCES user_account (id);
