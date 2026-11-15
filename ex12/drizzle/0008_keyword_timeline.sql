CREATE TABLE "keyword_devices" (
	"subscription_id" text PRIMARY KEY NOT NULL,
	"user_id" text NOT NULL,
	"session_id" text,
	"morning" boolean DEFAULT true NOT NULL,
	"seen" text[] DEFAULT '{}' NOT NULL
);

--> statement-breakpoint
ALTER TABLE "keyword_devices" ENABLE ROW LEVEL SECURITY;
--> statement-breakpoint
CREATE TABLE "saved_keywords" (
	"id" text PRIMARY KEY NOT NULL,
	"user_id" text NOT NULL,
	"name" text NOT NULL,
	"normalized" text NOT NULL,
	"color" integer NOT NULL,
	"alerts" boolean DEFAULT false NOT NULL,
	"baseline" text[] DEFAULT '{}' NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"enabled_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "saved_keywords_owner_name" UNIQUE("user_id","normalized"),
	CONSTRAINT "saved_keywords_owner_color" UNIQUE("user_id","color"),
	CONSTRAINT "saved_keywords_color_range" CHECK ("saved_keywords"."color" BETWEEN 0 AND 4)
);

--> statement-breakpoint
ALTER TABLE "saved_keywords" ENABLE ROW LEVEL SECURITY;
--> statement-breakpoint
ALTER TABLE "keyword_devices" ADD CONSTRAINT "keyword_devices_subscription_id_push_subscriptions_id_fk" FOREIGN KEY ("subscription_id") REFERENCES "public"."push_subscriptions"("id") ON DELETE cascade ON UPDATE no action;
--> statement-breakpoint
ALTER TABLE "keyword_devices" ADD CONSTRAINT "keyword_devices_user_id_user_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."user"("id") ON DELETE cascade ON UPDATE no action;
--> statement-breakpoint
ALTER TABLE "keyword_devices" ADD CONSTRAINT "keyword_devices_session_id_session_id_fk" FOREIGN KEY ("session_id") REFERENCES "public"."session"("id") ON DELETE set null ON UPDATE no action;
--> statement-breakpoint
ALTER TABLE "saved_keywords" ADD CONSTRAINT "saved_keywords_user_id_user_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."user"("id") ON DELETE cascade ON UPDATE no action;
