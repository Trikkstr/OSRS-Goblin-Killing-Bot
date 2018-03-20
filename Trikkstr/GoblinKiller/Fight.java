package Trikkstr.GoblinKiller;

import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.rt4.*;

import java.util.concurrent.Callable;
import java.util.regex.Pattern;

public class Fight extends Task
{

    final static int GOBLIN[] = { 3029, 3030, 3031, 3032, 3034, 3035 };
    final static int FOOD[] = { 315, 1971, 2309 };

    final static int HUT_VALUES[] = {3243, 3244, 3245, 3246, 3247, 3248};

    Component inventory = ctx.widgets.widget(161).component(61);

    public Fight(ClientContext ctx)
    {
        super(ctx);
    }

    @Override
    public boolean activate()
    {
        return true;
    }

    @Override
    public void execute()
    {
        System.out.printf("Executing Fight.\n");

        if(bonesInInventory() && !ctx.players.local().inCombat())
            dropBones();

        while(lootNearby() && !ctx.players.local().inCombat())
        {
            System.out.printf("Looting.\n");
            pickup();
        }

        if(hasFood())
        {
            System.out.printf(("Has Food: True\n"));

            if(needsHeal())
            {
                System.out.printf("Needs Heal: True\n");
                heal();
            }
            else if(shouldAttack())
            {
                System.out.printf("Should Attack: True\n");
                attack();
            }
        }
    }

    public boolean bonesInInventory()
    {
        return ctx.inventory.select().id(526).count() > 0;
    }

    public boolean needsHeal()
    {
        return ctx.combat.health() < 6;
    }

    public boolean shouldAttack()
    {
        return !ctx.players.local().inCombat();
    }

    public boolean hasFood()
    {
        return ctx.inventory.select().id(FOOD).count() > 0;
    }

    public boolean lootNearby()
    {
        GroundItem loot = ctx.groundItems.select().name(Pattern.compile("(.*rune)|(Coins)|(.*bolts)")).nearest().poll();
        return(loot.tile().distanceTo(ctx.players.local().tile()) <= 4);
    }

    public void pickup()
    {
        GroundItem loot = ctx.groundItems.select().name(Pattern.compile("(.*rune)|(Coins)|(.*bolts)")).nearest().poll();

        final int startingWealth = ctx.inventory.select().name(Pattern.compile("(.*rune)|(Coins)|(.*bolts)")).count(true);

        if(ctx.inventory.select().id(loot).count() > 0 || ctx.inventory.count() < 28)
        {
            ctx.camera.turnTo(loot);
            loot.interact("Take");
        }


        Condition.wait(new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws Exception
            {
                final int currentWealth = ctx.inventory.select().name(Pattern.compile("(.*rune)|(Coins)|(.*bolts)")).count(true);
                return currentWealth != startingWealth;
            }
        }, 400, 7);
    }

    public void attack()
    {
        System.out.printf("Selecting A Goblin To Attack\n");

        final Npc goblin = getGoblin();

        if(!goblin.inViewport());
        ctx.camera.turnTo(goblin);

        goblin.interact("Attack", "Goblin");

        Condition.wait(new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws Exception
            {

                return ctx.players.local().inCombat();
            }
        }, 500, 10);
    }

    public void heal()
    {
        inventory.click();

        Item food = ctx.inventory.select().id(FOOD).poll();

        final int startingHealth = ctx.combat.health();

        food.interact("Eat");

        Condition.wait(new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws Exception
            {
                final int currentHealth = ctx.combat.health();
                return currentHealth != startingHealth;
            }
        }, 150, 20);
    }

    public void dropBones()
    {
        for(Item bones : ctx.inventory.select().id(526))
        {
            if(ctx.controller.isStopping())
            {
                break;
            }

            final int startAmountInventory = ctx.inventory.select().count();
            bones.interact("Drop", "Bones");

            Condition.wait(new Callable<Boolean>()
            {
                @Override
                public Boolean call() throws Exception
                {
                    return ctx.inventory.select().count() != startAmountInventory;
                }
            }, 75, 20);
        }
    }

    public Npc getGoblin()
    {
        return ctx.npcs.select().id(GOBLIN).select(new Filter<Npc>()
        {
            @Override
            public boolean accept(Npc npc)
            {
                return !npc.inCombat();
            }
        }).select(new Filter<Npc>() {
            @Override
            public boolean accept(Npc npc)
            {
                boolean hit = false;
                int x = npc.tile().x();
                int y = npc.tile().y();
                for(int i = 0;  i < HUT_VALUES.length; i++ )
                {
                    if(x == HUT_VALUES[i])
                        for(int j = 0; j < HUT_VALUES.length; j++)
                            if(y == HUT_VALUES[j])
                                hit = true;
                }
                return hit == false;
            }
        }).select(new Filter<Npc>() {
            @Override
            public boolean accept(Npc npc)
            {
                int count = 0;
                int y = npc.tile().y();
                for(int i = 0;  i < HUT_VALUES.length; i++ )
                {
                    if(y == HUT_VALUES[i])
                        count += 1;
                }
                return count == 0;
            }
        }).nearest().poll();
    }
}
