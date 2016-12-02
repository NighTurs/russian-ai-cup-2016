import model.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WizardTraitsTest {

    private static final SkillType[] EMPTY_SKILLS = new SkillType[0];
    private static final Status[] EMPTY_STATUSES = new Status[0];
    private Game game;
    private Wizard wizard;

    @Before
    public void setUp() throws Exception {
        game = mock(Game.class);
        wizard = mock(Wizard.class);
    }

    @Test
    public void testGetWizardCastRangeRegular() throws Exception {
        when(game.getWizardCastRange()).thenReturn(500.0);
        when(game.getRangeBonusPerSkillLevel()).thenReturn(25.0);
        when(wizard.getSkills()).thenReturn(EMPTY_SKILLS);
        assertEquals(500.0, WizardTraits.getWizardCastRange(wizard, game), 0.0);
    }

    @Test
    public void testGetWizardCastRangeSkills() throws Exception {
        when(game.getWizardCastRange()).thenReturn(500.0);
        when(game.getRangeBonusPerSkillLevel()).thenReturn(25.0);
        when(wizard.getSkills()).thenReturn(new SkillType[]{SkillType.RANGE_BONUS_PASSIVE_1,
                SkillType.RANGE_BONUS_PASSIVE_2,
                SkillType.RANGE_BONUS_AURA_1,
                SkillType.RANGE_BONUS_AURA_2});
        assertEquals(600.0, WizardTraits.getWizardCastRange(wizard, game), 0.0);
    }

    @Test
    public void testGetMagicMissileDirectDamageRegular() throws Exception {
        when(game.getMagicMissileDirectDamage()).thenReturn(12);
        when(game.getMagicalDamageBonusPerSkillLevel()).thenReturn(1);
        when(game.getEmpoweredDamageFactor()).thenReturn(0.5);
        when(wizard.getSkills()).thenReturn(EMPTY_SKILLS);
        when(wizard.getStatuses()).thenReturn(EMPTY_STATUSES);
        assertEquals(12.0, WizardTraits.getMagicMissileDirectDamage(wizard, game), 0.0);
    }

    @Test
    public void testGetMagicMissileDirectDamageSkillsAndBonus() throws Exception {
        when(game.getMagicMissileDirectDamage()).thenReturn(12);
        when(game.getMagicalDamageBonusPerSkillLevel()).thenReturn(1);
        when(game.getEmpoweredDamageFactor()).thenReturn(0.5);
        when(wizard.getSkills()).thenReturn(new SkillType[]{SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_1,
                SkillType.MAGICAL_DAMAGE_BONUS_PASSIVE_2,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_1,
                SkillType.MAGICAL_DAMAGE_BONUS_AURA_2});
        when(wizard.getStatuses()).thenReturn(new Status[]{new Status(0, StatusType.EMPOWERED, 0, 0, 1)});
        assertEquals(24.0, WizardTraits.getMagicMissileDirectDamage(wizard, game), 0.0);
    }

    @Test
    public void testGetWizardForwardSpeedRegular() throws Exception {
        when(game.getWizardForwardSpeed()).thenReturn(4.0);
        when(game.getHastenedMovementBonusFactor()).thenReturn(0.3);
        when(game.getMovementBonusFactorPerSkillLevel()).thenReturn(0.05);
        when(wizard.getSkills()).thenReturn(EMPTY_SKILLS);
        when(wizard.getStatuses()).thenReturn(EMPTY_STATUSES);
        assertEquals(4.0, WizardTraits.getWizardForwardSpeed(wizard, game), 0.0);
    }

    @Test
    public void testGetWizardForwardSpeedSkillsAndBonus() throws Exception {
        when(game.getWizardForwardSpeed()).thenReturn(4.0);
        when(game.getHastenedMovementBonusFactor()).thenReturn(0.3);
        when(game.getMovementBonusFactorPerSkillLevel()).thenReturn(0.05);
        when(wizard.getSkills()).thenReturn(new SkillType[]{SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_1,
                SkillType.MOVEMENT_BONUS_FACTOR_PASSIVE_2,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_1,
                SkillType.MOVEMENT_BONUS_FACTOR_AURA_2});
        when(wizard.getStatuses()).thenReturn(new Status[]{new Status(0, StatusType.HASTENED, 0, 0, 1)});
        assertEquals(6.0, WizardTraits.getWizardForwardSpeed(wizard, game), 0.0);
    }
}