-- Drizzle migrator는 이 파일 전체를 트랜잭션에서 실행한다.
-- 검사 직후 무소유 쓰기가 유입되지 않도록 먼저 두 테이블의 쓰기를 잠근다.
LOCK TABLE "folders", "bookmarks" IN ACCESS EXCLUSIVE MODE;
--> statement-breakpoint
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM "folders") OR EXISTS (SELECT 1 FROM "bookmarks") THEN
    RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'OWNERSHIP_MIGRATION_REQUIRES_EMPTY_TABLES';
  END IF;
END $$;
--> statement-breakpoint
ALTER TABLE "bookmarks" DROP CONSTRAINT "bookmarks_issue_id_unique";
--> statement-breakpoint
ALTER TABLE "bookmarks" ADD COLUMN "user_id" text NOT NULL;
--> statement-breakpoint
ALTER TABLE "folders" ADD COLUMN "user_id" text NOT NULL;
--> statement-breakpoint
ALTER TABLE "bookmarks" ADD CONSTRAINT "bookmarks_user_id_user_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."user"("id") ON DELETE no action ON UPDATE no action;
--> statement-breakpoint
ALTER TABLE "folders" ADD CONSTRAINT "folders_user_id_id_unique" UNIQUE("user_id","id");
--> statement-breakpoint
ALTER TABLE "bookmarks" ADD CONSTRAINT "bookmarks_owner_folder_fk" FOREIGN KEY ("user_id","folder_id") REFERENCES "public"."folders"("user_id","id") ON DELETE no action ON UPDATE no action;
--> statement-breakpoint
ALTER TABLE "folders" ADD CONSTRAINT "folders_user_id_user_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."user"("id") ON DELETE no action ON UPDATE no action;
--> statement-breakpoint
ALTER TABLE "bookmarks" ADD CONSTRAINT "bookmarks_user_id_issue_id_unique" UNIQUE("user_id","issue_id");
